package com.nudo.app.util

/**
 * Parte la transcripción que manda el servidor. Cada intervención llega como
 * "[mm:ss] Hablante: lo que dijo", separadas por una línea en blanco.
 *
 * La parte pura (testeable en JVM) vive aquí; el pintado con spans lo hace
 * [TranscripcionEstilo] en la capa Android.
 */
data class LineaTranscripcion(
    val tiempo: String,
    val hablante: String,
    val texto: String,
)

private val PATRON_LINEA = Regex("""^\[(\d{1,2}:\d{2})]\s+([^:]+):\s*([\s\S]*)$""")

fun interpretarTranscripcion(transcripcion: String): List<LineaTranscripcion> =
    transcripcion.split("\n\n")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { bloque ->
            val partes = PATRON_LINEA.find(bloque)
            if (partes == null) {
                // Transcripciones antiguas o líneas sueltas: se muestran tal cual.
                LineaTranscripcion(tiempo = "", hablante = "", texto = bloque)
            } else {
                val (tiempo, hablante, texto) = partes.destructured
                LineaTranscripcion(tiempo = tiempo, hablante = hablante.trim(), texto = texto.trim())
            }
        }

/** Hablantes distintos, en orden de aparición: para colorear cada uno igual siempre. */
fun hablantesDe(lineas: List<LineaTranscripcion>): List<String> =
    lineas.map { it.hablante }.filter { it.isNotEmpty() }.distinct()
