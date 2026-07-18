package com.nudo.app.util

import android.graphics.Rect
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * Reserva el espacio de las barras del sistema (hora, notificaciones, barra de
 * navegación y notch/recorte de pantalla) sumándolo al padding original de la
 * vista. Así el contenido nunca queda tapado, sea cual sea el móvil.
 *
 * Guarda el padding de partida definido en el XML y le suma los insets cada vez
 * que cambian (rotación, gestos, teclado…), de forma que la separación base de la
 * interfaz se mantiene intacta.
 */
fun View.aplicarInsetsDeBarras() {
    val paddingBase = Rect(paddingLeft, paddingTop, paddingRight, paddingBottom)
    ViewCompat.setOnApplyWindowInsetsListener(this) { vista, insetsVentana ->
        val barras = insetsVentana.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
        )
        vista.updatePadding(
            left = paddingBase.left + barras.left,
            top = paddingBase.top + barras.top,
            right = paddingBase.right + barras.right,
            bottom = paddingBase.bottom + barras.bottom,
        )
        insetsVentana
    }
    ViewCompat.requestApplyInsets(this)
}
