package com.nudo.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.nudo.app.databinding.ActivityAjustesBinding
import com.nudo.app.util.aplicarInsetsDeBarras

/**
 * Ajustes: qué dispositivo es este y cómo retirarlo del servidor.
 *
 * Retirarse **borra las conversaciones de este dispositivo**, porque solo viven
 * en el servidor. Por eso la confirmación dice cuántas son, con su número, en
 * vez de un «esta acción es irreversible» que nadie lee.
 */
class AjustesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAjustesBinding
    private val viewModel: AjustesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityAjustesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.aplicarInsetsDeBarras()

        binding.botonAtras.setOnClickListener { finish() }
        binding.botonRetirarse.setOnClickListener { confirmarRetirada() }

        viewModel.nombreDispositivo.observe(this) { nombre ->
            binding.nombreDispositivo.text = nombre
        }
        viewModel.conversaciones.observe(this) { cuantas -> pintarRecuento(cuantas) }
        viewModel.retirada.observe(this) { estado -> pintarRetirada(estado) }

        viewModel.cargar()
    }

    private fun pintarRecuento(cuantas: Int?) {
        binding.detalleRetirarse.text = when {
            cuantas == null -> getString(R.string.ajustes_retirarse_sin_recuento)
            cuantas == 0 -> getString(R.string.ajustes_retirarse_sin_conversaciones)
            else -> resources.getQuantityString(R.plurals.ajustes_retirarse_detalle, cuantas, cuantas)
        }
    }

    private fun pintarRetirada(estado: EstadoRetirada) {
        binding.botonRetirarse.isEnabled = estado != EstadoRetirada.EnCurso
        binding.progresoRetirada.visibility =
            if (estado == EstadoRetirada.EnCurso) View.VISIBLE else View.GONE

        when (estado) {
            is EstadoRetirada.Hecha -> volverAlInicio()
            is EstadoRetirada.Fallida ->
                Toast.makeText(this, estado.mensaje, Toast.LENGTH_LONG).show()
            else -> Unit
        }
    }

    /**
     * La confirmación nombra el número exacto de conversaciones que se van. Si no
     * se ha podido contar (sin red), se dice también: prometer un número que no
     * se ha comprobado sería peor que no darlo.
     */
    private fun confirmarRetirada() {
        val cuantas = viewModel.conversaciones.value
        val mensaje = when {
            cuantas == null -> getString(R.string.ajustes_confirmar_sin_recuento)
            cuantas == 0 -> getString(R.string.ajustes_confirmar_sin_conversaciones)
            else -> resources.getQuantityString(R.plurals.ajustes_confirmar_mensaje, cuantas, cuantas)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.ajustes_confirmar_titulo)
            .setMessage(mensaje)
            .setPositiveButton(R.string.ajustes_confirmar_si) { _, _ -> viewModel.retirarse() }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }

    /** Sin credencial no hay historial que enseñar: se vuelve al inicio de cero. */
    private fun volverAlInicio() {
        Toast.makeText(this, R.string.ajustes_retirada_hecha, Toast.LENGTH_LONG).show()
        val inicio = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(inicio)
        finish()
    }
}
