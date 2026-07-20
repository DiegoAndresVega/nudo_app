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

class ConversacionViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val _detalle = MutableLiveData<Trabajo?>()
    val detalle: LiveData<Trabajo?> = _detalle

    private val _guardando = MutableLiveData(false)
    val guardando: LiveData<Boolean> = _guardando

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
                    avisar(R.string.aviso_lista_sin_conexion)
                    delay(INTERVALO_SONDEO_MS)
                    continue
                }
                _detalle.postValue(trabajo)
                if (trabajo.estado == "completado" || trabajo.estado == "error") return@launch
                delay(INTERVALO_SONDEO_MS)
            }
        }
    }

    /** El nombre actual de una etiqueta, o la etiqueta misma si no tiene alias. */
    fun nombreDe(etiqueta: String): String =
        _detalle.value?.nombres?.get(etiqueta) ?: etiqueta

    /**
     * Renombra un hablante. El servidor guarda el alias por etiqueta, así que el
     * cambio se aplica a todas las intervenciones de ese hablante a la vez.
     */
    fun renombrarHablante(etiqueta: String, nombre: String) {
        val id = idTrabajo ?: return
        val actual = _detalle.value ?: return
        if (_guardando.value == true) return

        val propuesto = nombre.trim()
        if (propuesto == actual.nombres[etiqueta].orEmpty()) return

        _guardando.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nombres = actual.nombres.toMutableMap().apply {
                    if (propuesto.isEmpty()) remove(etiqueta) else put(etiqueta, propuesto)
                }
                _detalle.postValue(ClienteNudo.renombrarHablantes(id, nombres))
            } catch (_: Exception) {
                avisar(R.string.aviso_renombrar_fallido)
            }
            _guardando.postValue(false)
        }
    }

    fun cambiarTitulo(titulo: String) {
        val id = idTrabajo ?: return
        val actual = _detalle.value ?: return
        val propuesto = titulo.trim()
        if (propuesto.isEmpty() || propuesto == actual.titulo || _guardando.value == true) return

        _guardando.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val guardado = ClienteNudo.cambiarTitulo(id, propuesto)
                _detalle.postValue(actual.copy(titulo = guardado))
            } catch (_: Exception) {
                avisar(R.string.aviso_titulo_fallido)
            }
            _guardando.postValue(false)
        }
    }

    fun consumirAviso() {
        _aviso.value = null
    }

    private fun avisar(mensaje: Int) {
        _aviso.postValue(getApplication<Application>().getString(mensaje))
    }
}
