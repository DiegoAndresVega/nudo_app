package com.nudo.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nudo.app.red.ClienteNudo
import com.nudo.app.red.Trabajo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val INTERVALO_SONDEO_MS = 15_000L

/** Clave interna de la vista de transcripción (no es un análisis de la API). */
const val VISTA_TRANSCRIPCION = "transcripcion"

class ConversacionViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val _detalle = MutableLiveData<Trabajo?>()
    val detalle: LiveData<Trabajo?> = _detalle

    private val _vistas = MutableLiveData<Map<String, String>>(emptyMap())
    val vistas: LiveData<Map<String, String>> = _vistas

    private val _vistaActiva = MutableLiveData<String?>()
    val vistaActiva: LiveData<String?> = _vistaActiva

    private val _hilando = MutableLiveData<String?>()
    val hilando: LiveData<String?> = _hilando

    private val _aviso = MutableLiveData<String?>()
    val aviso: LiveData<String?> = _aviso

    private var idTrabajo: String? = null

    fun cargar(id: String) {
        if (idTrabajo == id && _detalle.value?.estado == "completado") return
        idTrabajo = id
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val trabajo = try {
                    ClienteNudo.consultarTrabajo(id)
                } catch (_: Exception) {
                    _aviso.postValue(getApplication<Application>().getString(R.string.aviso_lista_sin_conexion))
                    delay(INTERVALO_SONDEO_MS)
                    continue
                }
                _detalle.postValue(trabajo)
                if (trabajo.estado == "completado") {
                    val vistas = trabajo.analisis +
                        (VISTA_TRANSCRIPCION to (trabajo.hablantes ?: ""))
                    _vistas.postValue(vistas)
                    _vistaActiva.postValue(
                        if (vistas.containsKey("resumen")) "resumen" else VISTA_TRANSCRIPCION,
                    )
                    return@launch
                }
                if (trabajo.estado == "error") return@launch
                delay(INTERVALO_SONDEO_MS)
            }
        }
    }

    /**
     * ¿La vista ya está lista? Si es así, seleccionarla solo cambia de pestaña
     * (gratis). Si no, [seleccionar] pedirá el análisis a la IA (gasto), así que
     * la pantalla puede confirmar antes. La transcripción siempre está lista.
     */
    fun yaGenerada(tipo: String): Boolean = _vistas.value.orEmpty().containsKey(tipo)

    fun seleccionar(tipo: String) {
        if (_hilando.value != null) return
        if (_vistas.value.orEmpty().containsKey(tipo)) {
            _vistaActiva.value = tipo
            return
        }
        val id = idTrabajo ?: return
        _hilando.value = tipo
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contenido = ClienteNudo.pedirAnalisis(id, tipo)
                _vistas.postValue(_vistas.value.orEmpty() + (tipo to contenido))
                _vistaActiva.postValue(tipo)
            } catch (_: Exception) {
                _aviso.postValue(getApplication<Application>().getString(R.string.aviso_hilado_fallido))
            }
            _hilando.postValue(null)
        }
    }

    fun consumirAviso() {
        _aviso.value = null
    }
}
