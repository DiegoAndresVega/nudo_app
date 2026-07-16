package com.nudo.app.util

/**
 * Interpretación mínima del Markdown que devuelve Claude: títulos, viñetas y
 * negritas. La parte pura (testeable en JVM) vive aquí; el pintado con spans
 * lo hace [Marcado.aSpanned] en la capa Android.
 */
enum class TipoLinea { TITULO, VINETA, NORMAL }

data class LineaMarcada(val texto: String, val tipo: TipoLinea)

data class TextoConNegritas(val texto: String, val negritas: List<IntRange>)

private val PATRON_TITULO = Regex("^#{1,6}\\s+")
private val PATRON_VINETA = Regex("^[-*•]\\s+")
private val PATRON_NEGRITA = Regex("\\*\\*(.+?)\\*\\*")

fun interpretarLineas(markdown: String): List<LineaMarcada> {
    val lineas = mutableListOf<LineaMarcada>()
    for (cruda in markdown.lines()) {
        val linea = cruda.trimEnd()
        when {
            linea.isBlank() -> {
                if (lineas.isNotEmpty() && lineas.last().texto.isNotEmpty()) {
                    lineas.add(LineaMarcada("", TipoLinea.NORMAL))
                }
            }
            PATRON_TITULO.containsMatchIn(linea) ->
                lineas.add(LineaMarcada(linea.replace(PATRON_TITULO, ""), TipoLinea.TITULO))
            PATRON_VINETA.containsMatchIn(linea.trimStart()) ->
                lineas.add(LineaMarcada(linea.trimStart().replace(PATRON_VINETA, ""), TipoLinea.VINETA))
            else -> lineas.add(LineaMarcada(linea, TipoLinea.NORMAL))
        }
    }
    while (lineas.isNotEmpty() && lineas.last().texto.isEmpty()) {
        lineas.removeAt(lineas.lastIndex)
    }
    return lineas
}

/** Quita los marcadores `**` y devuelve el texto limpio con los rangos en negrita. */
fun extraerNegritas(texto: String): TextoConNegritas {
    val negritas = mutableListOf<IntRange>()
    val limpio = StringBuilder()
    var cursor = 0
    for (coincidencia in PATRON_NEGRITA.findAll(texto)) {
        limpio.append(texto, cursor, coincidencia.range.first)
        val inicio = limpio.length
        limpio.append(coincidencia.groupValues[1])
        negritas.add(inicio until limpio.length)
        cursor = coincidencia.range.last + 1
    }
    limpio.append(texto, cursor, texto.length)
    return TextoConNegritas(limpio.toString(), negritas)
}
