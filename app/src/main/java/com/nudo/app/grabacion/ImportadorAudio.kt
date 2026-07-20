package com.nudo.app.grabacion

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.nudo.app.pendientes.ColaPendientes
import java.io.File
import java.io.IOException

/**
 * Trae a la cola un audio que ya estaba en el móvil (grabadora del sistema,
 * WhatsApp, descargas...). Se copia a almacenamiento interno porque el permiso
 * sobre el Uri elegido no sobrevive a un reintento posterior sin red.
 */
object ImportadorAudio {

    /** mp4 y 3gp son contenedores AAC: el servidor los entiende como m4a. */
    private val EQUIVALENCIAS = mapOf("mp4" to "m4a", "3gp" to "m4a", "oga" to "ogg")

    class FormatoNoSoportado(val extension: String) : IOException("Formato no soportado: $extension")

    fun importar(contexto: Context, uri: Uri): File {
        val extension = extensionDe(contexto, uri)
        val destino = ColaPendientes.crearArchivo(contexto, extension)
        try {
            contexto.contentResolver.openInputStream(uri).use { entrada ->
                if (entrada == null) throw IOException("No se pudo abrir el audio elegido")
                destino.outputStream().use { salida -> entrada.copyTo(salida) }
            }
        } catch (error: Exception) {
            destino.delete()
            throw error
        }
        if (destino.length() == 0L) {
            destino.delete()
            throw IOException("El audio elegido está vacío")
        }
        return destino
    }

    /** Público para poder probar la normalización sin un Uri real. */
    fun normalizarExtension(cruda: String): String {
        val limpia = cruda.lowercase().removePrefix(".")
        val equivalente = EQUIVALENCIAS[limpia] ?: limpia
        if (equivalente !in ColaPendientes.EXTENSIONES_ACEPTADAS) {
            throw FormatoNoSoportado(limpia.ifEmpty { "desconocido" })
        }
        return equivalente
    }

    private fun extensionDe(contexto: Context, uri: Uri): String {
        val nombre = nombreVisible(contexto, uri)
        val extensionCruda = nombre?.substringAfterLast('.', "").orEmpty().ifEmpty {
            contexto.contentResolver.getType(uri)?.substringAfterLast('/', "").orEmpty()
        }
        return normalizarExtension(extensionCruda)
    }

    private fun nombreVisible(contexto: Context, uri: Uri): String? =
        contexto.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
}
