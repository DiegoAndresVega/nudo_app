package com.nudo.app.red

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

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
 * No se cifra con el Android Keystore, de momento: eso protegería además frente a
 * un móvil rooteado o a una extracción física. La librería que lo hace fácil
 * (`androidx.security:security-crypto`) está deprecada y solo tiene versiones
 * alpha, así que queda pendiente hacerlo a mano contra el Keystore.
 */
object AlmacenCredencial {

    private const val PREFERENCIAS = "nudo.credencial"
    private const val CLAVE_TOKEN = "token"
    private const val CLAVE_ID = "idDispositivo"

    /** El servidor lo recorta a 60; se manda ya cortado para no provocar un 422. */
    private const val LONGITUD_MAXIMA_NOMBRE = 60

    private lateinit var preferencias: SharedPreferences

    fun iniciar(contexto: Context) {
        preferencias = contexto.applicationContext
            .getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
    }

    fun token(): String? = preferencias.getString(CLAVE_TOKEN, null)

    fun idDispositivo(): String? = preferencias.getString(CLAVE_ID, null)

    fun guardar(idDispositivo: String, token: String) {
        preferencias.edit()
            .putString(CLAVE_ID, idDispositivo)
            .putString(CLAVE_TOKEN, token)
            .apply()
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
