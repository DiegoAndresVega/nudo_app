package com.nudo.app

import com.nudo.app.red.ClienteNudo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClienteNudoTest {

    @Test
    fun `parsearId extrae el id del trabajo`() {
        val json = """{"id":"abc123","estado":"pendiente"}"""
        assertEquals("abc123", ClienteNudo.parsearId(json))
    }

    @Test
    fun `parsearLista devuelve los trabajos con titulo`() {
        val json = """{"trabajos":[
            {"id":"b","estado":"completado","creado":"2026-07-16T10:00:00+00:00","titulo":"Tutoría del TFG"},
            {"id":"a","estado":"pendiente","creado":"2026-07-16T09:00:00+00:00","titulo":"Sin título"}
        ]}"""
        val lista = ClienteNudo.parsearLista(json)
        assertEquals(2, lista.size)
        assertEquals("b", lista[0].id)
        assertEquals("Tutoría del TFG", lista[0].titulo)
        assertEquals("pendiente", lista[1].estado)
    }

    @Test
    fun `parsearTrabajo con trabajo pendiente y titulo nulo`() {
        val json = """{"id":"abc123","estado":"pendiente","creado":"2026-07-16T00:00:00+00:00","titulo":null}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("pendiente", trabajo.estado)
        assertNull(trabajo.titulo)
        assertNull(trabajo.hablantes)
        assertTrue(trabajo.analisis.isEmpty())
    }

    @Test
    fun `parsearTrabajo con trabajo completado incluye hablantes y analisis`() {
        val json = """{"id":"abc123","estado":"completado","titulo":"Prueba",
            "hablantes":"[00:00] SPEAKER_00: Hola",
            "analisis":{"resumen":"## Resumen generado"}}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("completado", trabajo.estado)
        assertEquals("Prueba", trabajo.titulo)
        assertEquals("[00:00] SPEAKER_00: Hola", trabajo.hablantes)
        assertEquals("## Resumen generado", trabajo.analisis["resumen"])
    }

    @Test
    fun `parsearTrabajo con error`() {
        val json = """{"id":"abc123","estado":"error","error":"fallo de prueba"}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("error", trabajo.estado)
        assertEquals("fallo de prueba", trabajo.error)
    }

    @Test
    fun `parsearAnalisis extrae el contenido`() {
        val json = """{"tipo":"resumen","contenido":"## Resumen\n- punto"}"""
        assertEquals("## Resumen\n- punto", ClienteNudo.parsearAnalisis(json))
    }
}
