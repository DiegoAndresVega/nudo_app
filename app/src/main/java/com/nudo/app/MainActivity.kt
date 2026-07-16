package com.nudo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.nudo.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NudoViewModel by viewModels()

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

        binding.botonPrincipal.setOnClickListener { alPulsarBotonPrincipal() }
        binding.botonReiniciar.setOnClickListener { viewModel.reiniciar() }

        viewModel.estado.observe(this) { estado -> pintarEstado(estado) }
    }

    private fun alPulsarBotonPrincipal() {
        when (viewModel.estado.value) {
            is EstadoUi.Inactivo, is EstadoUi.Error -> pedirGrabacion()
            is EstadoUi.Grabando -> viewModel.detenerYEnviar()
            else -> Unit
        }
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

    private fun pintarEstado(estado: EstadoUi) {
        binding.indicadorProgreso.visibility =
            if (estado is EstadoUi.Subiendo || estado is EstadoUi.Procesando) View.VISIBLE else View.GONE
        binding.botonReiniciar.visibility =
            if (estado is EstadoUi.Completado || estado is EstadoUi.Error) View.VISIBLE else View.GONE
        binding.contenedorResultado.visibility =
            if (estado is EstadoUi.Completado) View.VISIBLE else View.GONE
        binding.botonPrincipal.isEnabled =
            estado is EstadoUi.Inactivo || estado is EstadoUi.Grabando || estado is EstadoUi.Error

        when (estado) {
            is EstadoUi.Inactivo -> {
                binding.textoEstado.setText(R.string.estado_listo)
                binding.botonPrincipal.setText(R.string.boton_grabar)
            }
            is EstadoUi.Grabando -> {
                binding.textoEstado.setText(R.string.estado_grabando)
                binding.botonPrincipal.setText(R.string.boton_parar)
            }
            is EstadoUi.Subiendo -> {
                binding.textoEstado.setText(R.string.estado_subiendo)
                binding.botonPrincipal.setText(R.string.boton_grabar)
            }
            is EstadoUi.Procesando -> {
                binding.textoEstado.text = getString(R.string.estado_procesando, estado.estadoServidor)
                binding.botonPrincipal.setText(R.string.boton_grabar)
            }
            is EstadoUi.Completado -> {
                binding.textoEstado.setText(R.string.estado_completado)
                binding.textoNotas.text = estado.notas
                binding.textoHablantes.text = estado.hablantes
                binding.botonPrincipal.setText(R.string.boton_grabar)
            }
            is EstadoUi.Error -> {
                binding.textoEstado.text = estado.mensaje
                binding.botonPrincipal.setText(R.string.boton_reintentar)
            }
        }
    }
}
