package com.nudo.app.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.nudo.app.R
import java.util.ArrayDeque

private const val MAXIMO_BARRAS = 36
private const val AMPLITUD_TOPE = 24_000f

/** La onda de grabación: barras del hilo que avanzan con la voz. */
class OndaView @JvmOverloads constructor(
    contexto: Context,
    atributos: AttributeSet? = null,
) : View(contexto, atributos) {

    private val amplitudes = ArrayDeque<Float>(MAXIMO_BARRAS)
    private val pintura = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contexto.getColor(R.color.nudo_hilo)
        strokeCap = Paint.Cap.ROUND
    }
    private val anchoBarra = recursos(4f)
    private val hueco = recursos(3.5f)

    private fun recursos(dp: Float): Float = dp * resources.displayMetrics.density

    fun agregar(amplitud: Int) {
        if (amplitudes.size >= MAXIMO_BARRAS) amplitudes.removeFirst()
        amplitudes.addLast((amplitud / AMPLITUD_TOPE).coerceIn(0f, 1f))
        invalidate()
    }

    fun limpiar() {
        amplitudes.clear()
        invalidate()
    }

    override fun onDraw(lienzo: Canvas) {
        super.onDraw(lienzo)
        val centroY = height / 2f
        val alturaMinima = recursos(3f)
        val alturaMaxima = height * 0.9f
        var x = width / 2f + (amplitudes.size / 2f) * (anchoBarra + hueco)
        for (amplitud in amplitudes.reversed()) {
            val altura = (alturaMinima + amplitud * (alturaMaxima - alturaMinima)) / 2f
            lienzo.drawRoundRect(
                x - anchoBarra, centroY - altura, x, centroY + altura,
                anchoBarra / 2f, anchoBarra / 2f, pintura,
            )
            x -= anchoBarra + hueco
            if (x < 0) break
        }
    }
}
