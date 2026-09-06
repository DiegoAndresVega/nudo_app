package com.nudo.app.almacen

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * El cofre de verdad: AES/GCM con una clave que vive en el Android Keystore.
 *
 * La clave **no se puede exportar**. La guarda el sistema (en hardware si el
 * móvil tiene TEE o Titan M) y esta app solo puede pedirle que cifre o descifre,
 * nunca leerla. Por eso copiar el fichero de preferencias o el JSON de una
 * conversación —lo que consigue un móvil rooteado o una extracción física— ya no
 * basta: sin la clave, lo copiado no dice nada.
 *
 * Se hace a mano y no con `androidx.security:security-crypto`, que era la forma
 * fácil pero está **deprecada** y solo tiene versiones alpha (ver #27).
 *
 * GCM además autentica: si alguien cambia un byte del fichero, descifrar falla en
 * vez de devolver basura que parezca válida.
 */
object CofreKeystore : Cofre {

    private const val PROVEEDOR = "AndroidKeyStore"
    private const val ALIAS = "nudo.cofre"
    private const val TRANSFORMACION = "AES/GCM/NoPadding"

    /** GCM en Android usa 12 bytes de IV y 128 bits de etiqueta. */
    private const val BYTES_IV = 12
    private const val BITS_ETIQUETA = 128

    private const val BASE64 = Base64.NO_WRAP

    override fun cifrar(claro: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(Cipher.ENCRYPT_MODE, clave())
        // El IV lo genera el sistema (setRandomizedEncryptionRequired) y viaja
        // delante del texto cifrado: hace falta para descifrar y no es secreto.
        val cifrado = cifrador.doFinal(claro.toByteArray(Charsets.UTF_8))
        return MARCA_CIFRADO + Base64.encodeToString(cifrador.iv + cifrado, BASE64)
    }

    override fun descifrar(guardado: String): String? = try {
        val bytes = Base64.decode(guardado.removePrefix(MARCA_CIFRADO), BASE64)
        val cifrador = Cipher.getInstance(TRANSFORMACION)
        cifrador.init(
            Cipher.DECRYPT_MODE,
            clave(),
            GCMParameterSpec(BITS_ETIQUETA, bytes, 0, BYTES_IV),
        )
        String(
            cifrador.doFinal(bytes, BYTES_IV, bytes.size - BYTES_IV),
            Charsets.UTF_8,
        )
    } catch (_: Exception) {
        // Clave perdida, guardado de otra instalación o manipulado: no se puede
        // leer, y eso es una respuesta válida. Nunca devolver basura.
        null
    }

    /** La crea la primera vez y la reutiliza siempre. Nunca sale del Keystore. */
    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance(PROVEEDOR).apply { load(null) }
        (almacen.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR)
        generador.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                // Un IV repetido con la misma clave rompe GCM del todo, así que
                // se deja que lo genere el sistema en cada cifrado.
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generador.generateKey()
    }
}
