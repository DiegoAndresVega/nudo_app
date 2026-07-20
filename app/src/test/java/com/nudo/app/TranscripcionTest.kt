package com.nudo.app

import com.nudo.app.util.LineaTranscripcion
import com.nudo.app.util.hablantesDe
import com.nudo.app.util.interpretarTranscripcion
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscripcionTest {

    private val transcripcion = """
        [00:07] SPEAKER_00: Yo quitaría el estudio de campo.

        [00:19] SPEAKER_01: ¿Y lo compenso con datos del INE?

        [00:24] SPEAKER_00: Sí, y refuerzas metodología.
    """.trimIndent()

    @Test
    fun `parte cada intervencion en tiempo, hablante y texto`() {
        val lineas = interpretarTranscripcion(transcripcion)
        assertEquals(3, lineas.size)
        assertEquals(
            LineaTranscripcion("00:07", "SPEAKER_00", "Yo quitaría el estudio de campo."),
            lineas[0],
        )
        assertEquals("SPEAKER_01", lineas[1].hablante)
    }

    @Test
    fun `los hablantes salen sin repetir y en orden de aparicion`() {
        assertEquals(
            listOf("SPEAKER_00", "SPEAKER_01"),
            hablantesDe(interpretarTranscripcion(transcripcion)),
        )
    }

    @Test
    fun `entiende los nombres que ha puesto el usuario`() {
        val lineas = interpretarTranscripcion("[00:07] Diego Andrés: Buenas.")
        assertEquals("Diego Andrés", lineas[0].hablante)
        assertEquals("Buenas.", lineas[0].texto)
    }

    @Test
    fun `el texto puede contener dos puntos sin romper el parseo`() {
        val lineas = interpretarTranscripcion("[01:02] Diego: Mira: esto es la clave.")
        assertEquals("Diego", lineas[0].hablante)
        assertEquals("Mira: esto es la clave.", lineas[0].texto)
    }

    @Test
    fun `una linea que no encaja se muestra tal cual`() {
        val lineas = interpretarTranscripcion("una nota suelta sin formato")
        assertEquals(LineaTranscripcion("", "", "una nota suelta sin formato"), lineas[0])
    }

    @Test
    fun `una transcripcion vacia no da lineas`() {
        assertEquals(emptyList<LineaTranscripcion>(), interpretarTranscripcion("   "))
    }
}
