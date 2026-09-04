package com.nudo.app

import android.app.Application
import com.nudo.app.almacen.CopiaLocal
import com.nudo.app.red.AlmacenCredencial

/**
 * Existe para dejar listas la credencial y la copia local antes de que nadie
 * llame a la red, sin depender de qué pantalla se abra primero.
 */
class NudoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AlmacenCredencial.iniciar(this)
        CopiaLocal.iniciar(this)
    }
}
