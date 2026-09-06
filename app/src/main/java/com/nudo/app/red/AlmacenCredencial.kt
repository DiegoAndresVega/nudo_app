package com.nudo.app.red

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.nudo.app.almacen.Cofre
import com.nudo.app.almacen.CofreKeystore
import com.nudo.app.almacen.estaCifrado
import com.nudo.app.almacen.leerAunqueSeaViejo

/**
 * Guarda la credencial propia de esta instalación.
 *
 * Antes la app mandaba una clave compartida horneada en el APK: extraerla con
 * `apktool` daba acceso a las conversaciones de todo el mundo, y rotarla obligaba
 * a reinstalar en todos los móviles a la vez. Ahora cada instalación pide su
 * token al servidor y se puede revocar por separado.
 *
 * El token vive en las preferencias privadas de la app, que el sistema no deja
 * leer a otras aplicaciones, y `allowBackup="false"` impide que salga del
 * dispositivo en una copia de Google Drive.
 *
 * Y va **cifrado** contra el Android Keystore ([CofreKeystore]), que cubre lo que
 * el sandbox no cubre: un móvil rooteado o una extracción física. Se hace a mano
 * porque la librería que lo hacía fácil (`androidx.security:security-crypto`)
 * está deprecada y solo tiene versiones alpha (#27).
 */
object AlmacenCredencial {

    private const val PREFERENCIAS = "nudo.credencial"
    private const val CLAVE_TOKEN = "token"
    private const val CLAVE_ID = "idDispositivo"

    /** El servidor lo recorta a 60; se manda ya cortado para no provocar un 422. */
    private const val LONGITUD_MAXIMA_NOMBRE = 60

    private lateinit var preferencias: SharedPreferences
    private val cofre: Cofre = CofreKeystore

    fun iniciar(contexto: Context) {
        preferencias = contexto.applicationContext
            .getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
    }

    fun token(): String? = leer(CLAVE_TOKEN)

    fun idDispositivo(): String? = leer(CLAVE_ID)

    fun guardar(idDispositivo: String, token: String) {
        preferencias.edit()
            .putString(CLAVE_ID, cofre.cifrar(idDispositivo))
            .putString(CLAVE_TOKEN, cofre.cifrar(token))
            .apply()
    }

    /**
     * Si lo guardó una versión anterior, que escribía en claro, se reescribe
     * cifrado al leerlo. Así actualizar la app no obliga a darse de alta otra vez
     * —eso perdería el acceso a las conversaciones del servidor— y el token no se
     * queda en claro para siempre solo porque ya estuviera escrito.
     *
     * `null` si no se puede descifrar: el Keystore perdió la clave. `ClienteNudo`
     * ya sabe qué hacer con un token ausente, que es pedir uno nuevo.
     */
    private fun leer(clave: String): String? {
        val guardado = preferencias.getString(clave, null) ?: return null
        val valor = cofre.leerAunqueSeaViejo(guardado) ?: return null
        if (!estaCifrado(guardado)) {
            preferencias.edit().putString(clave, cofre.cifrar(valor)).apply()
        }
        return valor
    }

    /** Tras un 401: el token pudo revocarse desde otro sitio, así que se descarta. */
    fun olvidar() {
        preferencias.edit().remove(CLAVE_TOKEN).remove(CLAVE_ID).apply()
    }

    /** Nombre con el que se reconocerá este móvil en la lista de dispositivos. */
    fun nombreDeEsteDispositivo(): String = nombreLegible(Build.MANUFACTURER, Build.MODEL)

    /** Separado de [nombreDeEsteDispositivo] para poder probarlo sin Android. */
    fun nombreLegible(fabricante: String?, modelo: String?): String {
        val propuesto = listOfNotNull(fabricante, modelo)
            .joinToString(" ") { it.trim() }
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ")
        val nombre = propuesto.ifBlank { "Android" }
        return nombre.take(LONGITUD_MAXIMA_NOMBRE)
    }
}
