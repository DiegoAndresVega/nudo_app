package com.nudo.app

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nudo.app.databinding.ActivityConversacionBinding
import com.nudo.app.util.Fechas
import com.nudo.app.util.Marcado
import com.nudo.app.util.aplicarInsetsDeBarras

/** Orden de los chips: primero lo que más se usa, como en la identidad. */
private val TIPOS = listOf(
    "resumen" to R.string.tipo_resumen,
    VISTA_TRANSCRIPCION to R.string.tipo_transcripcion,
    "esquema" to R.string.tipo_esquema,
    "notas" to R.string.tipo_notas,
    "acciones" to R.string.tipo_acciones,
    "preguntas" to R.string.tipo_preguntas,
    "objetivos" to R.string.tipo_objetivos,
)

class ConversacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITULO = "titulo"
        const val EXTRA_CREADO = "creado"
    }

    private lateinit var binding: ActivityConversacionBinding
    private val viewModel: ConversacionViewModel by viewModels()
    private val chips = mutableMapOf<String, Chip>()

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
        binding.botonAtras.setOnClickListener { finish() }
        binding.tituloConversacion.text =
            intent.getStringExtra(EXTRA_TITULO) ?: getString(R.string.sin_titulo)
        binding.metaConversacion.text =
            intent.getStringExtra(EXTRA_CREADO)?.let { Fechas.formatearCreado(it) }.orEmpty()

        crearChips()
        observar()
        viewModel.cargar(id)
    }

    private fun crearChips() {
        for ((clave, etiqueta) in TIPOS) {
            val chip = Chip(this).apply {
                text = getString(etiqueta)
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = getColorStateList(R.color.chip_fondo)
                chipStrokeColor = getColorStateList(R.color.chip_trazo)
                chipStrokeWidth = resources.displayMetrics.density
                setTextColor(getColorStateList(R.color.chip_texto))
                setOnClickListener { alPulsarChip(clave) }
            }
            chips[clave] = chip
            binding.grupoChips.addView(chip)
        }
    }

    /**
     * La transcripción y lo ya hilado se muestran directos (gratis). Un análisis
     * que aún no existe se pide a la IA (gasto), así que primero se confirma.
     */
    private fun alPulsarChip(clave: String) {
        if (viewModel.yaGenerada(clave)) {
            viewModel.seleccionar(clave)
        } else {
            confirmarHilado(clave)
        }
    }

    private fun confirmarHilado(clave: String) {
        val etiqueta = getString(TIPOS.first { it.first == clave }.second)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirmar_hilar_titulo, etiqueta))
            .setMessage(R.string.confirmar_hilar_mensaje)
            .setNegativeButton(R.string.confirmar_hilar_no, null)
            .setPositiveButton(R.string.confirmar_hilar_si) { _, _ -> viewModel.seleccionar(clave) }
            .show()
    }

    private fun observar() {
        viewModel.detalle.observe(this) { trabajo ->
            if (trabajo == null) return@observe
            trabajo.titulo?.let { binding.tituloConversacion.text = it }
            when (trabajo.estado) {
                "completado" -> mostrarContenido(true)
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
        viewModel.vistaActiva.observe(this) { pintarVista() }
        viewModel.vistas.observe(this) { pintarVista() }
        viewModel.hilando.observe(this) { pintarChips() }
        viewModel.aviso.observe(this) { aviso ->
            if (aviso != null) {
                Toast.makeText(this, aviso, Toast.LENGTH_LONG).show()
                viewModel.consumirAviso()
            }
        }
    }

    private fun mostrarContenido(visible: Boolean) {
        binding.contenedorContenido.visibility = if (visible) View.VISIBLE else View.GONE
        binding.grupoChips.visibility = if (visible) View.VISIBLE else View.GONE
        binding.grupoEstadoDetalle.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun pintarVista() {
        pintarChips()
        val activa = viewModel.vistaActiva.value ?: return
        val contenido = viewModel.vistas.value.orEmpty()[activa] ?: return
        if (activa == VISTA_TRANSCRIPCION) {
            binding.textoContenido.typeface = Typeface.MONOSPACE
            binding.textoContenido.textSize = 13f
            binding.textoContenido.text = contenido
        } else {
            binding.textoContenido.typeface = Typeface.DEFAULT
            binding.textoContenido.textSize = 15f
            binding.textoContenido.text = Marcado.aSpanned(contenido)
        }
        binding.contenedorContenido.scrollTo(0, 0)
    }

    private fun pintarChips() {
        val activa = viewModel.vistaActiva.value
        val hilando = viewModel.hilando.value
        for ((clave, chip) in chips) {
            chip.isChecked = clave == activa
            val etiqueta = getString(TIPOS.first { it.first == clave }.second)
            chip.text =
                if (clave == hilando) etiqueta + getString(R.string.hilando_sufijo) else etiqueta
            chip.isEnabled = hilando == null
        }
    }
}
