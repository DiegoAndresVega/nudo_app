package com.nudo.app

import com.nudo.app.almacen.CopiaLocal
import com.nudo.app.red.Trabajo
import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CopiaLocalTest {

    private lateinit var directorio: File
    private val cofre = CofreDeMentira()

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
        CopiaLocal.guardarEn(directorio, conversacion, cofre)

        val recuperada = CopiaLocal.cargarDe(directorio, "abc123", cofre)

        assertEquals(conversacion, recuperada)
    }

    @Test
    fun `lo que llega al disco no es la conversacion en claro`() {
        CopiaLocal.guardarEn(directorio, conversacion, cofre)

        val enDisco = File(directorio, "abc123.json").readText()

        assertFalse(enDisco.contains("Tutoría del TFG"))
        assertFalse(enDisco.contains("qué tal"))
        assertFalse(enDisco.contains("Diego"))
    }

    @Test
    fun `una conversacion que no esta devuelve null`() {
        assertNull(CopiaLocal.cargarDe(directorio, "no-existe", cofre))
    }

    @Test
    fun `solo se guardan las completadas`() {
        // Una a medias no tiene nada que conservar, y guardarla haría creer que
        // hay copia de algo que todavía no existe.
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "aún", estado = "procesando"), cofre)

        assertNull(CopiaLocal.cargarDe(directorio, "aún", cofre))
    }

    @Test
    fun `lista de la mas reciente a la mas antigua`() {
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "vieja", creado = "2026-01-01T10:00:00+00:00"), cofre)
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "nueva", creado = "2026-09-01T10:00:00+00:00"), cofre)

        assertEquals(listOf("nueva", "vieja"), CopiaLocal.listarDe(directorio, cofre).map { it.id })
    }

    @Test
    fun `borrar quita solo esa`() {
        CopiaLocal.guardarEn(directorio, conversacion, cofre)
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "otra"), cofre)

        CopiaLocal.borrarDe(directorio, "abc123")

        assertNull(CopiaLocal.cargarDe(directorio, "abc123", cofre))
        assertNotNull(CopiaLocal.cargarDe(directorio, "otra", cofre))
    }

    @Test
    fun `vaciar deja el almacen limpio`() {
        CopiaLocal.guardarEn(directorio, conversacion, cofre)
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "otra"), cofre)

        CopiaLocal.vaciarEn(directorio)

        assertEquals(emptyList<Trabajo>(), CopiaLocal.listarDe(directorio, cofre))
    }

    @Test
    fun `un fichero corrupto no tumba el listado`() {
        // Un JSON a medias (batería agotada al escribir) no puede dejar sin
        // historial a todo lo demás.
        CopiaLocal.guardarEn(directorio, conversacion, cofre)
        File(directorio, "roto.json").writeText("{ esto no es json")

        assertEquals(listOf("abc123"), CopiaLocal.listarDe(directorio, cofre).map { it.id })
    }

    @Test
    fun `un id con barras no escapa del directorio`() {
        // El id lo devuelve el servidor, pero construye una ruta: si algún día
        // cambia, no puede acabar escribiendo fuera de su carpeta.
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "../fuera"), cofre)

        assertEquals(emptyList<Trabajo>(), CopiaLocal.listarDe(directorio, cofre))
        assertTrue(!File(directorio.parentFile, "fuera.json").exists())
    }

    // ---- Lo que dejó la versión anterior, que guardaba en claro ----

    @Test
    fun `una conversacion guardada en claro se sigue leyendo`() {
        // 0.8 escribía el JSON tal cual. Actualizar no puede perder el historial.
        File(directorio, "vieja.json").writeText(jsonEnClaro("vieja"))

        val recuperada = CopiaLocal.cargarDe(directorio, "vieja", cofre)

        assertEquals("Tutoría del TFG", recuperada?.titulo)
    }

    @Test
    fun `al leerla se reescribe cifrada`() {
        // Si no, una conversación de antes de esta versión se quedaría en claro
        // para siempre, y solo se cifraría lo nuevo.
        val fichero = File(directorio, "vieja.json")
        fichero.writeText(jsonEnClaro("vieja"))

        CopiaLocal.cargarDe(directorio, "vieja", cofre)

        assertFalse(fichero.readText().contains("Tutoría del TFG"))
        assertEquals("Tutoría del TFG", CopiaLocal.cargarDe(directorio, "vieja", cofre)?.titulo)
    }

    @Test
    fun `una conversacion cifrada que ya no se puede descifrar no tumba el listado`() {
        // Pasaría si el Keystore pierde la clave. Se pierde esa conversación,
        // igual que un fichero corrupto: el resto del historial sigue ahí.
        CopiaLocal.guardarEn(directorio, conversacion, cofre)
        CopiaLocal.guardarEn(directorio, conversacion.copy(id = "otra"), cofre)

        val sinClave = CofreDeMentira(sabeDescifrar = false)

        assertEquals(emptyList<Trabajo>(), CopiaLocal.listarDe(directorio, sinClave))
    }

    private fun jsonEnClaro(id: String): String = """
        {"id":"$id","estado":"completado","creado":"2026-09-04T10:00:00+00:00",
         "titulo":"Tutoría del TFG","hablantes":"Diego: hola","etiquetas":["SPEAKER_00"],
         "nombres":{"SPEAKER_00":"Diego"}}
    """.trimIndent()
}
