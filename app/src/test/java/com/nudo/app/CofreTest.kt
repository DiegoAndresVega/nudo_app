package com.nudo.app

import com.nudo.app.almacen.MARCA_CIFRADO
import com.nudo.app.almacen.estaCifrado
import com.nudo.app.almacen.leerAunqueSeaViejo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CofreTest {

    private val cofre = CofreDeMentira()

    @Test
    fun `lo cifrado va marcado y no es el texto original`() {
        val guardado = cofre.cifrar("token-secreto")

        assertTrue(estaCifrado(guardado))
        assertFalse(guardado.contains("token-secreto"))
    }

    @Test
    fun `lo cifrado vuelve entero al descifrarlo`() {
        assertEquals("token-secreto", cofre.descifrar(cofre.cifrar("token-secreto")))
    }

    @Test
    fun `lo que dejo una version anterior se lee tal cual`() {
        assertEquals("token-en-claro", cofre.leerAunqueSeaViejo("token-en-claro"))
    }

    @Test
    fun `un valor marcado se descifra al leerlo`() {
        val guardado = cofre.cifrar("token-secreto")

        assertEquals("token-secreto", cofre.leerAunqueSeaViejo(guardado))
    }

    @Test
    fun `un valor marcado que no se puede descifrar da null`() {
        val cofreSinClave = CofreDeMentira(sabeDescifrar = false)

        assertNull(cofreSinClave.leerAunqueSeaViejo(MARCA_CIFRADO + "loquesea"))
    }

    @Test
    fun `sin marca no esta cifrado`() {
        assertFalse(estaCifrado("token-en-claro"))
        assertFalse(estaCifrado(""))
    }
}
