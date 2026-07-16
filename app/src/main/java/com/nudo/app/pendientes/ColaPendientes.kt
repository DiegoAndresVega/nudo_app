package com.nudo.app.pendientes

import android.content.Context
import java.io.File

/**
 * Grabaciones que aún no han llegado al servidor. Viven en almacenamiento
 * interno hasta que la subida termina bien; si no hay red, sobreviven al
 * cierre de la app y se reintentan.
 */
object ColaPendientes {

    private const val CARPETA = "pendientes"

    fun carpeta(contexto: Context): File =
        File(contexto.filesDir, CARPETA).apply { mkdirs() }

    fun crearArchivo(contexto: Context): File =
        File(carpeta(contexto), "nudo_${System.currentTimeMillis()}.m4a")

    fun listar(contexto: Context): List<File> =
        carpeta(contexto).listFiles { archivo -> archivo.extension == "m4a" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
