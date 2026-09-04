package com.nudo.app

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nudo.app.databinding.ActivityConversacionBinding
import com.nudo.app.databinding.DialogoTextoBinding
import com.nudo.app.red.Trabajo
import com.nudo.app.util.Fechas
import com.nudo.app.util.TranscripcionEstilo
import com.nudo.app.util.aplicarInsetsDeBarras

/** Tope de caracteres del nombre de un hablante (igual que en api/transcripcion.py). */
private const val MAXIMO_NOMBRE = 40

/** Tope del título de la conversación (igual que en api/trabajos.py). */
private const val MAXIMO_TITULO = 80

class ConversacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_CREADO = "creado"
    }

    private lateinit var binding: ActivityConversacionBinding
    private val viewModel: ConversacionViewModel by viewModels()
    private var creado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityConversacionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.aplicarInsetsDeBarras()

        val id = intent.getStringExtra(EXTRA_ID)
        if (id == null) {
            finish()
            return
        }
        creado = intent.getStringExtra(EXTRA_CREADO)
        binding.botonAtras.setOnClickListener { finish() }
        binding.zonaTitulo.setOnClickListener { pedirTitulo() }
        binding.botonProximamente.setOnClickListener { mostrarProximamente() }
        binding.botonBorrar.setOnClickListener { confirmarBorrado() }

        pintarCabecera(intent.getStringExtra(EXTRA_TITULO))
        observar()
        viewModel.cargar(id)
    }

    private fun observar() {
        viewModel.detalle.observe(this) { trabajo ->
            if (trabajo != null) pintarTrabajo(trabajo)
        }
        viewModel.aviso.observe(this) { aviso ->
            if (aviso != null) {
                Toast.makeText(this, aviso, Toast.LENGTH_LONG).show()
                viewModel.consumirAviso()
            }
        }
        viewModel.borrando.observe(this) { borrando ->
            binding.botonBorrar.isEnabled = !borrando
        }
        viewModel.borrada.observe(this) { borrada ->
            if (borrada) {
                Toast.makeText(this, R.string.borrar_hecho, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // ---- Borrar ----

    /**
     * Confirmación antes de borrar. Se va todo de golpe —audio, transcripción y
     * nombres— y no hay copia en el móvil, así que el diálogo lo dice en vez de
     * limitarse a preguntar si está seguro.
     *
     * Al volver, el historial se recarga solo en `onResume`, así que la
     * conversación desaparece de la lista sin tocarla a mano.
     */
    private fun confirmarBorrado() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.borrar_titulo)
            .setMessage(R.string.borrar_mensaje)
            .setPositiveButton(R.string.borrar_confirmar) { _, _ -> viewModel.borrar() }
            .setNegativeButton(R.string.accion_cancelar, null)
            .show()
    }

    private fun pintarTrabajo(trabajo: Trabajo) {
        creado = trabajo.creado ?: creado
        pintarCabecera(trabajo.titulo)
        when (trabajo.estado) {
            "completado" -> {
                mostrarContenido(true)
                binding.textoContenido.text =
                    TranscripcionEstilo.aSpanned(this, trabajo.hablantes.orEmpty())
                pintarChipsHablantes(trabajo)
            }
            "error" -> {
                mostrarContenido(false)
                binding.progresoDetalle.visibility = View.GONE
                binding.textoEstadoDetalle.text =
                    getString(R.string.detalle_error, trabajo.error ?: "")
            }
            else -> {
                mostrarContenido(false)
                binding.progresoDetalle.visibility = View.VISIBLE
                binding.textoEstadoDetalle.setText(R.string.detalle_anudando)
            }
        }
    }

    /** Sin IA no hay título automático: si el usuario no ha puesto uno, se muestra la fecha. */
    private fun pintarCabecera(titulo: String?) {
        val fecha = creado?.let { Fechas.formatearCreado(it) }.orEmpty()
        binding.tituloConversacion.text = titulo ?: fecha.ifEmpty { getString(R.string.sin_titulo) }
        binding.metaConversacion.text = if (titulo == null) {
            getString(R.string.meta_toca_para_titular)
        } else {
            getString(R.string.meta_con_fecha, fecha)
        }
    }

    private fun mostrarContenido(visible: Boolean) {
        binding.contenedorContenido.visibility = if (visible) View.VISIBLE else View.GONE
        binding.grupoEstadoDetalle.visibility = if (visible) View.GONE else View.VISIBLE
    }

    // ---- Hablantes ----

    private fun pintarChipsHablantes(trabajo: Trabajo) {
        binding.grupoHablantes.visibility =
            if (trabajo.etiquetas.isEmpty()) View.GONE else View.VISIBLE
        binding.grupoChipsHablantes.removeAllViews()

        for ((indice, etiqueta) in trabajo.etiquetas.withIndex()) {
            val nombre = trabajo.nombres[etiqueta] ?: etiqueta
            val chip = Chip(this).apply {
                text = nombre
                isCheckable = false
                isCheckedIconVisible = false
                chipBackgroundColor = getColorStateList(R.color.chip_fondo)
                chipStrokeColor = getColorStateList(R.color.chip_trazo)
                chipStrokeWidth = resources.displayMetrics.density
                setTextColor(TranscripcionEstilo.colorDeHablante(this@ConversacionActivity, indice))
                contentDescription = getString(R.string.desc_renombrar_hablante, nombre)
                setOnClickListener { pedirNombreHablante(etiqueta, nombre) }
            }
            binding.grupoChipsHablantes.addView(chip)
        }
    }

    private fun pedirNombreHablante(etiqueta: String, nombreActual: String) {
        val valorInicial = if (nombreActual == etiqueta) "" else nombreActual
        pedirTexto(
            titulo = getString(R.string.renombrar_titulo, etiqueta),
            ayuda = getString(R.string.renombrar_ayuda),
            pista = getString(R.string.renombrar_pista),
            valorInicial = valorInicial,
            maximo = MAXIMO_NOMBRE,
        ) { nuevo -> viewModel.renombrarHablante(etiqueta, nuevo) }
    }

    // ---- Título ----

    private fun pedirTitulo() {
        val actual = viewModel.detalle.value?.titulo.orEmpty()
        pedirTexto(
            titulo = getString(R.string.titulo_dialogo),
            ayuda = null,
            pista = getString(R.string.titulo_pista),
            valorInicial = actual,
            maximo = MAXIMO_TITULO,
        ) { nuevo -> viewModel.cambiarTitulo(nuevo) }
    }

    // ---- Próximamente ----

    private fun mostrarProximamente() {
        val funciones = resources.getStringArray(R.array.proximamente_funciones)
            .joinToString("\n") { "•  $it" }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.proximamente_titulo)
            .setMessage(getString(R.string.proximamente_mensaje, funciones))
            .setPositiveButton(R.string.proximamente_cerrar, null)
            .show()
    }

    // ---- Diálogo de texto reutilizable ----

    private fun pedirTexto(
        titulo: String,
        ayuda: String?,
        pista: String,
        valorInicial: String,
        maximo: Int,
        alConfirmar: (String) -> Unit,
    ) {
        val dialogo = DialogoTextoBinding.inflate(layoutInflater)
        dialogo.contenedorCampo.hint = pista
        dialogo.contenedorCampo.counterMaxLength = maximo
        dialogo.contenedorCampo.isCounterEnabled = true
        dialogo.campoTexto.setText(valorInicial)
        dialogo.campoTexto.setSelection(valorInicial.length)

        MaterialAlertDialogBuilder(this)
            .setTitle(titulo)
            .apply { if (ayuda != null) setMessage(ayuda) }
            .setView(dialogo.root)
            .setNegativeButton(R.string.accion_cancelar, null)
            .setPositiveButton(R.string.accion_guardar) { _, _ ->
                alConfirmar(dialogo.campoTexto.text?.toString().orEmpty().take(maximo))
            }
            .show()
    }
}
