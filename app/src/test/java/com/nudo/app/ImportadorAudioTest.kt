package com.nudo.app

import com.nudo.app.grabacion.ImportadorAudio
import com.nudo.app.pendientes.ColaPendientes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportadorAudioTest {

    @Test
    fun `acepta las extensiones que entiende la API`() {
        assertEquals("mp3", ImportadorAudio.normalizarExtension("mp3"))
        assertEquals("opus", ImportadorAudio.normalizarExtension("OPUS"))
        assertEquals("m4a", ImportadorAudio.normalizarExtension(".m4a"))
    }

    @Test
    fun `traduce los contenedores equivalentes`() {
        assertEquals("m4a", ImportadorAudio.normalizarExtension("mp4"))
        assertEquals("m4a", ImportadorAudio.normalizarExtension("3gp"))
        assertEquals("ogg", ImportadorAudio.normalizarExtension("oga"))
    }

    @Test
    fun `la lista es exactamente la que acepta la API`() {
        assertEquals(
            setOf("m4a", "mp3", "aac", "ogg", "opus"),
            ColaPendientes.EXTENSIONES_ACEPTADAS,
        )
    }

    @Test
    fun `los formatos retirados ya no se aceptan`() {
        listOf("wav", "flac", "webm").forEach { extension ->
            val error = assertThrows(ImportadorAudio.FormatoNoSoportado::class.java) {
                ImportadorAudio.normalizarExtension(extension)
            }
            assertEquals(extension, error.extension)
        }
    }

    @Test
    fun `rechaza lo que la API no aceptaría`() {
        val error = assertThrows(ImportadorAudio.FormatoNoSoportado::class.java) {
            ImportadorAudio.normalizarExtension("pdf")
        }
        assertEquals("pdf", error.extension)
    }

    @Test
    fun `un archivo sin extension se rechaza con un motivo legible`() {
        val error = assertThrows(ImportadorAudio.FormatoNoSoportado::class.java) {
            ImportadorAudio.normalizarExtension("")
        }
        assertEquals("desconocido", error.extension)
    }
}
