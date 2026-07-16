package com.nudo.app.util

import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

/** Convierte el Markdown de los análisis en texto con estilo para un TextView. */
object Marcado {

    fun aSpanned(markdown: String): CharSequence {
        val resultado = SpannableStringBuilder()
        for (linea in interpretarLineas(markdown)) {
            val inicioLinea = resultado.length
            when (linea.tipo) {
                TipoLinea.TITULO -> {
                    resultado.append(linea.texto)
                    resultado.setSpan(
                        StyleSpan(Typeface.BOLD),
                        inicioLinea,
                        resultado.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    resultado.setSpan(
                        RelativeSizeSpan(1.12f),
                        inicioLinea,
                        resultado.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }
                TipoLinea.VINETA -> anadirConNegritas(resultado, "•  " + linea.texto)
                TipoLinea.NORMAL -> anadirConNegritas(resultado, linea.texto)
            }
            resultado.append("\n")
        }
        return resultado
    }

    private fun anadirConNegritas(destino: SpannableStringBuilder, texto: String) {
        val base = destino.length
        val (limpio, negritas) = extraerNegritas(texto)
        destino.append(limpio)
        for (rango in negritas) {
            destino.setSpan(
                StyleSpan(Typeface.BOLD),
                base + rango.first,
                base + rango.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }
}
