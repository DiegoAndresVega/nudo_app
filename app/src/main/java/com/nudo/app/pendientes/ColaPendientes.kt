package com.nudo.app.pendientes

import android.content.Context
import java.io.File

/**
 * Grabaciones que aún no han llegado al servidor. Viven en almacenamiento
 * interno hasta que la subida termina bien; si no hay red, sobreviven al
 * cierre de la app y se reintentan.
 *
 * Aquí caen tanto lo grabado con el micro (.m4a) como los audios que el
 * usuario importa desde el móvil, que pueden venir en otros formatos.
 */
object ColaPendientes {

    private const val CARPETA = "pendientes"

    const val EXTENSION_GRABACION = "m4a"

    /** Las mismas que acepta la API (ver api/config.py: EXTENSIONES_PERMITIDAS). */
    val EXTENSIONES_ACEPTADAS = setOf("m4a", "mp3", "wav", "ogg", "opus", "aac", "flac", "webm")

    fun carpeta(contexto: Context): File =
        File(contexto.filesDir, CARPETA).apply { mkdirs() }

    fun crearArchivo(contexto: Context, extension: String = EXTENSION_GRABACION): File =
        File(carpeta(contexto), "nudo_${System.currentTimeMillis()}.$extension")

    fun listar(contexto: Context): List<File> =
        carpeta(contexto)
            .listFiles { archivo -> archivo.extension.lowercase() in EXTENSIONES_ACEPTADAS }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
