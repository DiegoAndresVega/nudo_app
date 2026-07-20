package com.nudo.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nudo.app.grabacion.GrabadoraAudio
import com.nudo.app.grabacion.ImportadorAudio
import com.nudo.app.pendientes.ColaPendientes
import com.nudo.app.red.ClienteNudo
import com.nudo.app.red.TrabajoResumen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INTERVALO_SONDEO_MS = 15_000L
private const val INTERVALO_AMPLITUD_MS = 120L
private val ESTADOS_EN_CURSO = setOf("pendiente", "procesando")

sealed class EstadoGrabacion {
    data object Inactiva : EstadoGrabacion()
    data class Grabando(val segundos: Int) : EstadoGrabacion()
    data object Subiendo : EstadoGrabacion()
}

sealed class ItemHistorial {
    data class Remoto(val trabajo: TrabajoResumen) : ItemHistorial()
    data class Pendiente(val archivo: File) : ItemHistorial()
}

class HistorialViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val _estadoGrabacion = MutableLiveData<EstadoGrabacion>(EstadoGrabacion.Inactiva)
    val estadoGrabacion: LiveData<EstadoGrabacion> = _estadoGrabacion

    private val _items = MutableLiveData<List<ItemHistorial>>(emptyList())
    val items: LiveData<List<ItemHistorial>> = _items

    private val _amplitud = MutableLiveData<Int>()
    val amplitud: LiveData<Int> = _amplitud

    private val _aviso = MutableLiveData<String?>()
    val aviso: LiveData<String?> = _aviso

    private val grabadora = GrabadoraAudio()
    private var archivoActual: File? = null
    private var trabajoGrabacion: Job? = null
    private var trabajoSondeo: Job? = null

    init {
        cargarHistorial(reintentarPendientes = true)
    }

    fun iniciarGrabacion() {
        val contexto = getApplication<Application>()
        val archivo = ColaPendientes.crearArchivo(contexto)
        try {
            grabadora.iniciar(contexto, archivo)
        } catch (error: Exception) {
            archivo.delete()
            _aviso.value = contexto.getString(R.string.error_grabacion, error.message ?: "")
            return
        }
        archivoActual = archivo
        _estadoGrabacion.value = EstadoGrabacion.Grabando(0)
        trabajoGrabacion = viewModelScope.launch {
            var transcurridoMs = 0L
            while (true) {
                delay(INTERVALO_AMPLITUD_MS)
                transcurridoMs += INTERVALO_AMPLITUD_MS
                _amplitud.value = grabadora.amplitudMaxima()
                _estadoGrabacion.value = EstadoGrabacion.Grabando((transcurridoMs / 1000L).toInt())
            }
        }
    }

    fun detenerYEnviar() {
        trabajoGrabacion?.cancel()
        val contexto = getApplication<Application>()
        val archivo = archivoActual
        archivoActual = null
        try {
            grabadora.detener()
        } catch (error: Exception) {
            archivo?.delete()
            _aviso.value = contexto.getString(R.string.error_grabacion, error.message ?: "")
            _estadoGrabacion.value = EstadoGrabacion.Inactiva
            return
        }
        if (archivo == null || !archivo.exists()) {
            _aviso.value = contexto.getString(R.string.error_grabacion, "archivo perdido")
            _estadoGrabacion.value = EstadoGrabacion.Inactiva
            return
        }
        _estadoGrabacion.value = EstadoGrabacion.Subiendo
        viewModelScope.launch(Dispatchers.IO) {
            subir(archivo)
            _estadoGrabacion.postValue(EstadoGrabacion.Inactiva)
            cargarHistorial()
        }
    }

    /** Un audio que ya estaba en el móvil: se copia a la cola y sigue el mismo camino. */
    fun importarAudio(uri: Uri) {
        val contexto = getApplication<Application>()
        _estadoGrabacion.value = EstadoGrabacion.Subiendo
        viewModelScope.launch(Dispatchers.IO) {
            val archivo = try {
                ImportadorAudio.importar(contexto, uri)
            } catch (error: ImportadorAudio.FormatoNoSoportado) {
                _aviso.postValue(contexto.getString(R.string.error_formato, error.extension))
                _estadoGrabacion.postValue(EstadoGrabacion.Inactiva)
                return@launch
            } catch (error: Exception) {
                _aviso.postValue(contexto.getString(R.string.error_importar, error.message ?: ""))
                _estadoGrabacion.postValue(EstadoGrabacion.Inactiva)
                return@launch
            }
            subir(archivo)
            _estadoGrabacion.postValue(EstadoGrabacion.Inactiva)
            cargarHistorial()
        }
    }

    fun reintentarPendiente(archivo: File) {
        viewModelScope.launch(Dispatchers.IO) {
            subir(archivo)
            cargarHistorial()
        }
    }

    /**
     * Sube y borra el local solo si el servidor lo aceptó; si no, se queda en la
     * cola. Los reintentos automáticos al abrir la app van con [avisar] a false:
     * si no hay red, no tiene sentido soltar un aviso por cada pendiente.
     */
    private fun subir(archivo: File, avisar: Boolean = true): Boolean =
        try {
            ClienteNudo.subirAudio(archivo)
            archivo.delete()
            true
        } catch (_: Exception) {
            if (avisar) {
                _aviso.postValue(getApplication<Application>().getString(R.string.aviso_sin_conexion))
            }
            false
        }

    fun cargarHistorial(reintentarPendientes: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val contexto = getApplication<Application>()
            if (reintentarPendientes) {
                ColaPendientes.listar(contexto).forEach { archivo -> subir(archivo, avisar = false) }
            }
            val pendientes = ColaPendientes.listar(contexto).map { ItemHistorial.Pendiente(it) }
            val remotos = try {
                ClienteNudo.listarTrabajos().map { ItemHistorial.Remoto(it) }
            } catch (_: Exception) {
                if (_items.value.orEmpty().any { it is ItemHistorial.Remoto }) {
                    _aviso.postValue(contexto.getString(R.string.aviso_lista_sin_conexion))
                    _items.value.orEmpty().filterIsInstance<ItemHistorial.Remoto>()
                } else {
                    emptyList()
                }
            }
            _items.postValue(pendientes + remotos)
            programarSondeo(remotos)
        }
    }

    fun consumirAviso() {
        _aviso.value = null
    }

    private fun programarSondeo(remotos: List<ItemHistorial.Remoto>) {
        trabajoSondeo?.cancel()
        if (remotos.none { it.trabajo.estado in ESTADOS_EN_CURSO }) return
        trabajoSondeo = viewModelScope.launch {
            delay(INTERVALO_SONDEO_MS)
            cargarHistorial()
        }
    }

    override fun onCleared() {
        grabadora.cancelar()
        super.onCleared()
    }
}
