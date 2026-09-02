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
            {"id":"a","estado":"pendiente","creado":"2026-07-16T09:00:00+00:00","titulo":null}
        ]}"""
        val lista = ClienteNudo.parsearLista(json)
        assertEquals(2, lista.size)
        assertEquals("b", lista[0].id)
        assertEquals("Tutoría del TFG", lista[0].titulo)
        assertNull("sin IA, un trabajo recién hecho no trae título", lista[1].titulo)
    }

    @Test
    fun `parsearTrabajo con trabajo pendiente y titulo nulo`() {
        val json = """{"id":"abc123","estado":"pendiente","creado":"2026-07-16T00:00:00+00:00","titulo":null}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("pendiente", trabajo.estado)
        assertNull(trabajo.titulo)
        assertNull(trabajo.hablantes)
        assertTrue(trabajo.etiquetas.isEmpty())
        assertTrue(trabajo.nombres.isEmpty())
    }

    @Test
    fun `parsearTrabajo completado incluye transcripcion y etiquetas`() {
        val json = """{"id":"abc123","estado":"completado","titulo":"Prueba",
            "hablantes":"[00:00] SPEAKER_00: Hola",
            "etiquetas":["SPEAKER_00","SPEAKER_01"],
            "nombres":{}}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("completado", trabajo.estado)
        assertEquals("[00:00] SPEAKER_00: Hola", trabajo.hablantes)
        assertEquals(listOf("SPEAKER_00", "SPEAKER_01"), trabajo.etiquetas)
    }

    @Test
    fun `parsearTrabajo recoge los nombres puestos a los hablantes`() {
        val json = """{"id":"abc123","estado":"completado",
            "hablantes":"[00:00] Diego: Hola",
            "etiquetas":["SPEAKER_00"],
            "nombres":{"SPEAKER_00":"Diego"}}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals(mapOf("SPEAKER_00" to "Diego"), trabajo.nombres)
    }

    @Test
    fun `parsearTrabajo con error`() {
        val json = """{"id":"abc123","estado":"error","error":"fallo de prueba"}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("error", trabajo.estado)
        assertEquals("fallo de prueba", trabajo.error)
    }

    @Test
    fun `la respuesta del alta trae id y token`() {
        val json = """{"id":"d1f2","nombre":"Google Pixel 8","creado":"2026-09-02T10:00:00+00:00","token":"tok-abc"}"""
        val objeto = org.json.JSONObject(json)
        assertEquals("d1f2", objeto.getString("id"))
        assertEquals("tok-abc", objeto.getString("token"))
    }
}
