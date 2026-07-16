package com.nudo.app

import com.nudo.app.util.TipoLinea
import com.nudo.app.util.extraerNegritas
import com.nudo.app.util.interpretarLineas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarcadorTest {

    @Test
    fun `interpretarLineas distingue titulos vinetas y texto normal`() {
        val markdown = "## Resumen\n\n- Primer punto\n* Segundo punto\nPárrafo normal"
        val lineas = interpretarLineas(markdown)
        assertEquals(TipoLinea.TITULO, lineas[0].tipo)
        assertEquals("Resumen", lineas[0].texto)
        assertEquals(TipoLinea.VINETA, lineas[2].tipo)
        assertEquals("Primer punto", lineas[2].texto)
        assertEquals(TipoLinea.VINETA, lineas[3].tipo)
        assertEquals(TipoLinea.NORMAL, lineas[4].tipo)
        assertEquals("Párrafo normal", lineas[4].texto)
    }

    @Test
    fun `interpretarLineas colapsa lineas en blanco consecutivas`() {
        val lineas = interpretarLineas("uno\n\n\n\ndos")
        assertEquals(3, lineas.size)
        assertEquals("", lineas[1].texto)
    }

    @Test
    fun `interpretarLineas descarta lineas vacias finales`() {
        val lineas = interpretarLineas("uno\n\n")
        assertEquals(1, lineas.size)
    }

    @Test
    fun `extraerNegritas quita los marcadores y devuelve los rangos`() {
        val resultado = extraerNegritas("Decisión: **entregar el viernes** sin falta")
        assertEquals("Decisión: entregar el viernes sin falta", resultado.texto)
        assertEquals(1, resultado.negritas.size)
        val rango = resultado.negritas[0]
        assertEquals("entregar el viernes", resultado.texto.substring(rango.first, rango.last + 1))
    }

    @Test
    fun `extraerNegritas sin marcadores devuelve el texto intacto`() {
        val resultado = extraerNegritas("Texto plano")
        assertEquals("Texto plano", resultado.texto)
        assertTrue(resultado.negritas.isEmpty())
    }
}
