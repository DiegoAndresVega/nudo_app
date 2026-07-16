package com.nudo.app

import com.nudo.app.red.ClienteNudo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClienteNudoTest {

    @Test
    fun `parsearId extrae el id del trabajo`() {
        val json = """{"id":"abc123","estado":"pendiente"}"""
        assertEquals("abc123", ClienteNudo.parsearId(json))
    }

    @Test
    fun `parsearTrabajo con trabajo pendiente`() {
        val json = """{"id":"abc123","estado":"pendiente","creado":"2026-07-16T00:00:00"}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("pendiente", trabajo.estado)
        assertNull(trabajo.notas)
        assertNull(trabajo.error)
    }

    @Test
    fun `parsearTrabajo con trabajo completado`() {
        val json =
            """{"id":"abc123","estado":"completado","notas":"# Notas","hablantes":"[00:00] SPEAKER_00: Hola"}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("completado", trabajo.estado)
        assertEquals("# Notas", trabajo.notas)
        assertEquals("[00:00] SPEAKER_00: Hola", trabajo.hablantes)
    }

    @Test
    fun `parsearTrabajo con error`() {
        val json = """{"id":"abc123","estado":"error","error":"fallo de prueba"}"""
        val trabajo = ClienteNudo.parsearTrabajo(json)
        assertEquals("error", trabajo.estado)
        assertEquals("fallo de prueba", trabajo.error)
    }
}
