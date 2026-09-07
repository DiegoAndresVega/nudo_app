package com.nudo.app

import com.nudo.app.almacen.RellenoCopias
import com.nudo.app.red.Trabajo
import com.nudo.app.red.TrabajoResumen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests del relleno de copias (#29).
 *
 * La copia local de #18 solo se escribía al abrir una conversación, así que lo
 * que nunca se abrió nunca se copió: de 6 conversaciones en el servidor, en modo
 * avión se veía 1.
 */
class RellenoCopiasTest {

    private fun resumen(id: String, estado: String = "completado") =
        TrabajoResumen(id = id, estado = estado, creado = "2026-09-0$id", titulo = null)

    private fun conversacion(id: String) =
        Trabajo(id = id, estado = "completado", hablantes = "SPEAKER_00: hola")

    /** Recoge lo pedido y lo guardado para poder afirmar sobre el orden. */
    private class Espia(val fallanEstas: Set<String> = emptySet()) {
        val pedidas = mutableListOf<String>()
        val guardadas = mutableListOf<String>()

        fun traer(id: String): Trabajo {
            pedidas += id
            if (id in fallanEstas) throw RuntimeException("no se pudo traer $id")
            return Trabajo(id = id, estado = "completado", hablantes = "SPEAKER_00: hola")
        }

        fun guardar(trabajo: Trabajo) {
            guardadas += trabajo.id
        }
    }

    @Test
    fun `copia las que estan en el servidor y no en el movil`() {
        val espia = Espia()

        val copiadas = RellenoCopias.rellenar(
            resumenes = listOf(resumen("1"), resumen("2")),
            yaCopiadas = emptySet(),
            traer = espia::traer,
            guardar = espia::guardar,
        )

        assertEquals(2, copiadas)
        assertEquals(listOf("1", "2"), espia.guardadas)
    }

    @Test
    fun `no vuelve a pedir lo que ya esta copiado`() {
        // Es lo que hace que converja a cero: la primera carga rellena, las
        // siguientes no gastan ni una peticion.
        val espia = Espia()

        val copiadas = RellenoCopias.rellenar(
            resumenes = listOf(resumen("1"), resumen("2")),
            yaCopiadas = setOf("1"),
            traer = espia::traer,
            guardar = espia::guardar,
        )

        assertEquals(1, copiadas)
        assertEquals(listOf("2"), espia.pedidas)
    }

    @Test
    fun `no toca las que siguen transcribiendose`() {
        // Una a medias no tiene nada que conservar, y copiarla haria creer que
        // hay copia de algo que todavia no existe.
        val espia = Espia()

        val copiadas = RellenoCopias.rellenar(
            resumenes = listOf(resumen("1", estado = "procesando"), resumen("2", estado = "pendiente")),
            yaCopiadas = emptySet(),
            traer = espia::traer,
            guardar = espia::guardar,
        )

        assertEquals(0, copiadas)
        assertTrue(espia.pedidas.isEmpty())
    }

    @Test
    fun `una que falla no deja sin copia a las demas`() {
        // Sin esto, la conversacion rota de en medio dejaria a oscuras a todas
        // las que vienen detras, y encima en silencio.
        val espia = Espia(fallanEstas = setOf("2"))

        val copiadas = RellenoCopias.rellenar(
            resumenes = listOf(resumen("1"), resumen("2"), resumen("3")),
            yaCopiadas = emptySet(),
            traer = espia::traer,
            guardar = espia::guardar,
        )

        assertEquals(2, copiadas)
        assertEquals(listOf("1", "3"), espia.guardadas)
    }

    @Test
    fun `las pide de una en una y en orden`() {
        // El VPS es de 1 vCPU y hay limite de tasa por credencial (#6): quien
        // tenga cien conversaciones no puede lanzar cien peticiones a la vez ni
        // ganarse un 429 por intentar salvaguardar lo suyo.
        val espia = Espia()

        RellenoCopias.rellenar(
            resumenes = listOf(resumen("1"), resumen("2"), resumen("3")),
            yaCopiadas = emptySet(),
            traer = espia::traer,
            guardar = espia::guardar,
        )

        assertEquals(listOf("1", "2", "3"), espia.pedidas)
    }

    @Test
    fun `sin nada en el servidor no pide nada`() {
        // Es lo que pasa al abrir la app en avion: la lista viene vacia porque
        // no se pudo consultar, y eso no puede desatar peticiones ni avisos.
        val espia = Espia()

        val copiadas = RellenoCopias.rellenar(
            resumenes = emptyList(),
            yaCopiadas = setOf("1"),
            traer = espia::traer,
            guardar = espia::guardar,
        )

        assertEquals(0, copiadas)
        assertTrue(espia.pedidas.isEmpty())
    }
}
