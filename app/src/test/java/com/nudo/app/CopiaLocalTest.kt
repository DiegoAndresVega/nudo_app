package com.nudo.app

import com.nudo.app.almacen.CopiaLocal
import com.nudo.app.red.Trabajo
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CopiaLocalTest {

    private lateinit var directorio: File

    private val conversacion = Trabajo(
        id = "abc123",
        estado = "completado",
        creado = "2026-09-04T10:00:00+00:00",
        titulo = "Tutoría del TFG",
        hablantes = "Diego: hola\nAlberto: qué tal",
        etiquetas = listOf("SPEAKER_00", "SPEAKER_01"),
        nombres = mapOf("SPEAKER_00" to "Diego"),
    )

    @Before
    fun crearDirectorio() {
        directorio = Files.createTempDirectory("copia").toFile()
    }

    @After
    fun borrarDirectorio() {
        directorio.deleteRecursively()
    }

    @Test
    fun `guarda y recupera una conversacion entera`() {
        CopiaLocal.guardarEn(directorio, conversacion)

        val recuperada = CopiaLocal.cargarDe(directorio, "abc123")

        assertEquals(conversacion, recuperada)
    }

    @Test
    fun `una conversacion que no esta devuelve null`() {
        assertNull(CopiaLocal.cargarDe(directorio, "no-existe"))
    }

    @Test
    fun `solo se guardan las completadas`() {
        // Una a medias no tiene nada que conservar, y guardarla haría creer que
        // hay copia de algo que todavía no existe.
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "aún", estado = "procesando"))

        assertNull(CopiaLocal.cargarDe(directorio, "aún"))
    }

    @Test
    fun `lista de la mas reciente a la mas antigua`() {
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "vieja", creado = "2026-01-01T10:00:00+00:00"))
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "nueva", creado = "2026-09-01T10:00:00+00:00"))

        assertEquals(listOf("nueva", "vieja"), CopiaLocal.listarDe(directorio).map { it.id })
    }

    @Test
    fun `borrar quita solo esa`() {
        CopiaLocal.guardarEn(directorio, conversacion)
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "otra"))

        CopiaLocal.borrarDe(directorio, "abc123")

        assertNull(CopiaLocal.cargarDe(directorio, "abc123"))
        assertNotNull(CopiaLocal.cargarDe(directorio, "otra"))
    }

    @Test
    fun `vaciar deja el almacen limpio`() {
        CopiaLocal.guardarEn(directorio, conversacion)
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "otra"))

        CopiaLocal.vaciarEn(directorio)

        assertEquals(emptyList<Trabajo>(), CopiaLocal.listarDe(directorio))
    }

    @Test
    fun `un fichero corrupto no tumba el listado`() {
        // Un JSON a medias (batería agotada al escribir) no puede dejar sin
        // historial a todo lo demás.
        CopiaLocal.guardarEn(directorio, conversacion)
        File(directorio, "roto.json").writeText("{ esto no es json")

        assertEquals(listOf("abc123"), CopiaLocal.listarDe(directorio).map { it.id })
    }

    @Test
    fun `un id con barras no escapa del directorio`() {
        // El id lo devuelve el servidor, pero construye una ruta: si algún día
        // cambia, no puede acabar escribiendo fuera de su carpeta.
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "../fuera"))

        assertEquals(emptyList<Trabajo>(), CopiaLocal.listarDe(directorio))
        assertTrue(!File(directorio.parentFile, "fuera.json").exists())
    }
}
