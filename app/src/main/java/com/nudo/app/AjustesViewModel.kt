package com.nudo.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nudo.app.red.AlmacenCredencial
import com.nudo.app.red.ClienteNudo
import com.nudo.app.red.RetiradaEnEspera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** En qué punto está la retirada, para que la pantalla no tenga que deducirlo. */
sealed interface EstadoRetirada {
    data object Inactiva : EstadoRetirada
    data object EnCurso : EstadoRetirada
    data object Hecha : EstadoRetirada
    data class Fallida(val mensaje: Int) : EstadoRetirada
}

class AjustesViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val _nombreDispositivo = MutableLiveData<String>()
    val nombreDispositivo: LiveData<String> = _nombreDispositivo

    /** Cuántas conversaciones se perderán al retirarse. `null` mientras se cuenta. */
    private val _conversaciones = MutableLiveData<Int?>()
    val conversaciones: LiveData<Int?> = _conversaciones

    private val _retirada = MutableLiveData<EstadoRetirada>(EstadoRetirada.Inactiva)
    val retirada: LiveData<EstadoRetirada> = _retirada

    fun cargar() {
        _nombreDispositivo.value = AlmacenCredencial.nombreDeEsteDispositivo()
        viewModelScope.launch(Dispatchers.IO) {
            val cuantas = try {
                ClienteNudo.listarTrabajos().size
            } catch (_: Exception) {
                null
            }
            _conversaciones.postValue(cuantas)
        }
    }

    /**
     * Retira el dispositivo. El servidor borra con él sus conversaciones: como el
     * móvil no guarda copia, dejarlas allí sin credencial que las alcance las
     * convertiría en algo que ya nadie puede ni ver ni borrar.
     */
    fun retirarse() {
        if (_retirada.value == EstadoRetirada.EnCurso) return
        _retirada.value = EstadoRetirada.EnCurso
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ClienteNudo.retirarEsteDispositivo()
                _retirada.postValue(EstadoRetirada.Hecha)
            } catch (_: RetiradaEnEspera) {
                _retirada.postValue(EstadoRetirada.Fallida(R.string.ajustes_retirada_en_curso))
            } catch (_: Exception) {
                _retirada.postValue(EstadoRetirada.Fallida(R.string.ajustes_retirada_fallida))
            }
        }
    }
}
