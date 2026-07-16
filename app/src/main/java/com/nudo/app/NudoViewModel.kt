package com.nudo.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nudo.app.grabacion.GrabadoraAudio
import com.nudo.app.red.ClienteNudo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INTERVALO_SONDEO_MS = 15_000L

sealed class EstadoUi {
    data object Inactivo : EstadoUi()
    data object Grabando : EstadoUi()
    data object Subiendo : EstadoUi()
    data class Procesando(val idTrabajo: String, val estadoServidor: String) : EstadoUi()
    data class Completado(val notas: String, val hablantes: String) : EstadoUi()
    data class Error(val mensaje: String) : EstadoUi()
}

class NudoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val _estado = MutableLiveData<EstadoUi>(EstadoUi.Inactivo)
    val estado: LiveData<EstadoUi> = _estado

    private val grabadora = GrabadoraAudio()
    private var archivoActual: File? = null

    fun iniciarGrabacion() {
        val contexto = getApplication<Application>()
        val archivo = File(contexto.cacheDir, "grabacion_${System.currentTimeMillis()}.m4a")
        try {
            grabadora.iniciar(contexto, archivo)
        } catch (error: Exception) {
            _estado.value = EstadoUi.Error("No se pudo iniciar la grabación: ${error.message}")
            return
        }
        archivoActual = archivo
        _estado.value = EstadoUi.Grabando
    }

    fun detenerYEnviar() {
        val archivo = archivoActual
        try {
            grabadora.detener()
        } catch (error: Exception) {
            _estado.value = EstadoUi.Error("Fallo al terminar la grabación: ${error.message}")
            return
        }
        if (archivo == null || !archivo.exists()) {
            _estado.value = EstadoUi.Error("No se encontró el archivo grabado")
            return
        }
        _estado.value = EstadoUi.Subiendo
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val idTrabajo = ClienteNudo.subirAudio(archivo)
                archivo.delete()
                sondearTrabajo(idTrabajo)
            } catch (error: Exception) {
                _estado.postValue(EstadoUi.Error("Fallo al subir el audio: ${error.message}"))
            }
        }
    }

    fun reiniciar() {
        grabadora.cancelar()
        archivoActual?.delete()
        archivoActual = null
        _estado.value = EstadoUi.Inactivo
    }

    private suspend fun sondearTrabajo(idTrabajo: String) {
        while (true) {
            val trabajo = try {
                ClienteNudo.consultarTrabajo(idTrabajo)
            } catch (error: Exception) {
                _estado.postValue(EstadoUi.Procesando(idTrabajo, "sin conexión, reintentando"))
                delay(INTERVALO_SONDEO_MS)
                continue
            }
            when (trabajo.estado) {
                "completado" -> {
                    _estado.postValue(
                        EstadoUi.Completado(
                            notas = trabajo.notas ?: "(sin notas)",
                            hablantes = trabajo.hablantes ?: "(sin transcripción)",
                        ),
                    )
                    return
                }
                "error" -> {
                    _estado.postValue(
                        EstadoUi.Error("El servidor no pudo procesar el audio: ${trabajo.error ?: "error desconocido"}"),
                    )
                    return
                }
                else -> _estado.postValue(EstadoUi.Procesando(idTrabajo, trabajo.estado))
            }
            delay(INTERVALO_SONDEO_MS)
        }
    }

    override fun onCleared() {
        grabadora.cancelar()
        super.onCleared()
    }
}
