package com.nudo.app

import com.nudo.app.red.AlmacenCredencial
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlmacenCredencialTest {

    @Test
    fun `el nombre junta fabricante y modelo`() {
        assertEquals("Google Pixel 8", AlmacenCredencial.nombreLegible("Google", "Pixel 8"))
    }

    @Test
    fun `el nombre se queda en uno solo si el otro falta`() {
        assertEquals("Pixel 8", AlmacenCredencial.nombreLegible(null, "Pixel 8"))
        assertEquals("Google", AlmacenCredencial.nombreLegible("Google", null))
    }

    @Test
    fun `sin datos del dispositivo se usa un nombre por defecto`() {
        assertEquals("Android", AlmacenCredencial.nombreLegible(null, null))
        assertEquals("Android", AlmacenCredencial.nombreLegible("  ", " "))
    }

    @Test
    fun `los espacios de sobra se recortan`() {
        assertEquals("Google Pixel 8", AlmacenCredencial.nombreLegible("  Google ", "  Pixel   8 "))
    }

    @Test
    fun `un nombre larguisimo se corta al maximo que acepta el servidor`() {
        val nombre = AlmacenCredencial.nombreLegible("F".repeat(50), "M".repeat(50))
        assertTrue("no debe pasar de 60: era ${nombre.length}", nombre.length <= 60)
    }
}
