package com.nudo.app

import com.nudo.app.almacen.Cofre
import com.nudo.app.almacen.MARCA_CIFRADO

/**
 * Cofre de pruebas. El de verdad ([com.nudo.app.almacen.CofreKeystore]) necesita
 * el Keystore de Android y no arranca en la JVM, así que aquí se sustituye por
 * algo que hace lo mismo de mentira: marca lo guardado y le da la vuelta.
 *
 * Basta para lo que se prueba: que lo que llega al disco no es el texto original,
 * que vuelve entero al leerlo, y que un guardado ilegible no rompe nada.
 */
class CofreDeMentira(private val sabeDescifrar: Boolean = true) : Cofre {

    override fun cifrar(claro: String): String = MARCA_CIFRADO + claro.reversed()

    override fun descifrar(guardado: String): String? {
        if (!sabeDescifrar || !guardado.startsWith(MARCA_CIFRADO)) return null
        return guardado.removePrefix(MARCA_CIFRADO).reversed()
    }
}
