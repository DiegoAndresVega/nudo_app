package com.nudo.app.util

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import com.nudo.app.R

/**
 * Pinta la transcripción: el tiempo en gris apagado, el hablante en mono y con
 * su color, lo dicho en texto normal. Cada hablante mantiene su color en toda
 * la conversación para poder seguir el hilo de un vistazo.
 */
object TranscripcionEstilo {

    /** Paleta de la identidad. Si hay más hablantes que colores, se repiten. */
    private val COLORES_HABLANTE = listOf(
        R.color.nudo_hilo,
        R.color.nudo_atada,
        R.color.nudo_anudando,
        R.color.nudo_texto_suave,
    )

    fun colorDeHablante(contexto: Context, posicion: Int): Int =
        contexto.getColor(COLORES_HABLANTE[posicion % COLORES_HABLANTE.size])

    fun aSpanned(contexto: Context, transcripcion: String): CharSequence {
        val lineas = interpretarTranscripcion(transcripcion)
        val hablantes = hablantesDe(lineas)
        val resultado = SpannableStringBuilder()

        for ((indice, linea) in lineas.withIndex()) {
            if (indice > 0) resultado.append("\n\n")
            if (linea.tiempo.isNotEmpty()) {
                anadir(
                    resultado,
                    "${linea.tiempo}  ",
                    TypefaceSpan("monospace"),
                    RelativeSizeSpan(0.82f),
                    ForegroundColorSpan(contexto.getColor(R.color.nudo_texto_suave)),
                )
            }
            if (linea.hablante.isNotEmpty()) {
                val color = colorDeHablante(contexto, hablantes.indexOf(linea.hablante))
                anadir(
                    resultado,
                    "${linea.hablante}  ",
                    TypefaceSpan("monospace"),
                    RelativeSizeSpan(0.82f),
                    StyleSpan(Typeface.BOLD),
                    ForegroundColorSpan(color),
                )
            }
            resultado.append(linea.texto)
        }
        return resultado
    }

    private fun anadir(destino: SpannableStringBuilder, texto: String, vararg estilos: Any) {
        val inicio = destino.length
        destino.append(texto)
        for (estilo in estilos) {
            destino.setSpan(estilo, inicio, destino.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
