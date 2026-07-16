package com.nudo.app

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.nudo.app.databinding.ActivityMainBinding
import com.nudo.app.ui.AdaptadorHistorial
import com.nudo.app.util.Fechas
import java.util.Locale

private const val DURACION_PULSO_MS = 1_200L
private const val ESCALA_PULSO = 1.3f

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: HistorialViewModel by viewModels()
    private var animadorPulso: ValueAnimator? = null

    private val adaptador = AdaptadorHistorial { item ->
        when (item) {
            is ItemHistorial.Remoto -> abrirConversacion(item)
            is ItemHistorial.Pendiente -> viewModel.reintentarPendiente(item.archivo)
        }
    }

    private val pedirPermisoMicrofono = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            viewModel.iniciarGrabacion()
        } else {
            Toast.makeText(this, R.string.permiso_microfono_denegado, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.textoFecha.text = Fechas.hoy()
        binding.listaConversaciones.layoutManager = LinearLayoutManager(this)
        binding.listaConversaciones.adapter = adaptador

        binding.botonGrabar.setOnClickListener { pedirGrabacion() }
        binding.botonParar.setOnClickListener { viewModel.detenerYEnviar() }

        viewModel.estadoGrabacion.observe(this) { estado -> pintarEstado(estado) }
        viewModel.items.observe(this) { items ->
            adaptador.submitList(items)
            binding.grupoVacio.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
        viewModel.amplitud.observe(this) { amplitud -> binding.vistaOnda.agregar(amplitud) }
        viewModel.aviso.observe(this) { aviso ->
            if (aviso != null) {
                Toast.makeText(this, aviso, Toast.LENGTH_LONG).show()
                viewModel.consumirAviso()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.cargarHistorial()
    }

    private fun abrirConversacion(item: ItemHistorial.Remoto) {
        val intent = Intent(this, ConversacionActivity::class.java)
            .putExtra(ConversacionActivity.EXTRA_ID, item.trabajo.id)
            .putExtra(ConversacionActivity.EXTRA_TITULO, item.trabajo.titulo)
            .putExtra(ConversacionActivity.EXTRA_CREADO, item.trabajo.creado)
        startActivity(intent)
    }

    private fun pedirGrabacion() {
        val tienePermiso = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (tienePermiso) {
            viewModel.iniciarGrabacion()
        } else {
            pedirPermisoMicrofono.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun pintarEstado(estado: EstadoGrabacion) {
        binding.grupoHistorial.visibility =
            if (estado is EstadoGrabacion.Inactiva) View.VISIBLE else View.GONE
        binding.grupoGrabacion.visibility =
            if (estado is EstadoGrabacion.Grabando) View.VISIBLE else View.GONE
        binding.grupoSubiendo.visibility =
            if (estado is EstadoGrabacion.Subiendo) View.VISIBLE else View.GONE

        if (estado is EstadoGrabacion.Grabando) {
            binding.textoCronometro.text = String.format(
                Locale.ROOT, "%02d:%02d", estado.segundos / 60, estado.segundos % 60,
            )
            arrancarPulso()
        } else {
            pararPulso()
            binding.vistaOnda.limpiar()
        }
    }

    /** El botón late mientras graba: un anillo que se suelta cada 1,2 s. */
    private fun arrancarPulso() {
        if (animadorPulso != null) return
        animadorPulso = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURACION_PULSO_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animador ->
                val progreso = animador.animatedValue as Float
                val escala = 1f + (ESCALA_PULSO - 1f) * progreso
                binding.anilloPulso.scaleX = escala
                binding.anilloPulso.scaleY = escala
                binding.anilloPulso.alpha = 1f - progreso
            }
            start()
        }
    }

    private fun pararPulso() {
        animadorPulso?.cancel()
        animadorPulso = null
        binding.anilloPulso.alpha = 0f
    }

    override fun onDestroy() {
        pararPulso()
        super.onDestroy()
    }
}
