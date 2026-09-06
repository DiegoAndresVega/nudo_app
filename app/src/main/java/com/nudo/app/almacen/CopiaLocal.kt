package com.nudo.app.almacen

import android.content.Context
import com.nudo.app.red.Trabajo
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/**
 * Copia local de las conversaciones ya transcritas.
 *
 * Hasta ahora la transcripción vivía **solo** en el servidor: el audio se borra
 * al terminar y el móvil no guardaba nada, así que perder el volumen del VPS era
 * perderlo todo, sin forma de recuperarlo de ninguna parte (#18).
 *
 * También es la dirección a la que va Nudo. El servidor es andamiaje: cuando la
 * transcripción se haga en el propio móvil, este es el sitio natural del dato.
 * Y de paso, retirar el dispositivo deja de llevarse el historial por delante.
 *
 * Se guarda en el almacenamiento privado de la app, que el sistema no deja leer
 * a otras aplicaciones, y con `allowBackup="false"` no sale del móvil en una
 * copia de Drive. Es el mismo trato que la credencial: ver `AlmacenCredencial`.
 *
 * Y va **cifrado** contra el Android Keystore ([CofreKeystore]). Eso es lo que
 * cubre lo que el sandbox no cubre: un móvil rooteado o una extracción física del
 * almacenamiento. Pasó a hacer falta cuando el móvil empezó a guardar las
 * conversaciones y no solo la credencial: la credencial es revocable, una
 * conversación no (#27).
 *
 * Las funciones con `directorio` y `cofre` explícitos existen para poder probarlas
 * sin Android; las de arriba son las que usa la app.
 */
object CopiaLocal {

    private const val CARPETA = "conversaciones"

    /**
     * El id viene del servidor, pero aquí construye una ruta. Sin puntos ni
     * barras no hay forma de salir de la carpeta, y el juego de caracteres es
     * ancho a propósito: lo que se comprueba es que no escape, no de qué formato
     * es el id — eso lo decide el servidor y puede cambiar.
     */
    private val ID_VALIDO = Regex("[0-9a-zA-Z_-]{1,64}")

    private lateinit var directorio: File

    fun iniciar(contexto: Context) {
        directorio = File(contexto.applicationContext.filesDir, CARPETA)
    }

    fun guardar(trabajo: Trabajo) = guardarEn(directorio, trabajo, CofreKeystore)

    fun cargar(idTrabajo: String): Trabajo? = cargarDe(directorio, idTrabajo, CofreKeystore)

    fun listar(): List<Trabajo> = listarDe(directorio, CofreKeystore)

    fun borrar(idTrabajo: String) = borrarDe(directorio, idTrabajo)

    fun vaciar() = vaciarEn(directorio)

    // ---- Lo de abajo no toca Android, así que se puede probar en la JVM ----

    /**
     * Guarda una conversación **ya terminada**. Una a medias no tiene nada que
     * conservar, y dejarla haría creer que hay copia de algo que aún no existe.
     */
    fun guardarEn(carpeta: File, trabajo: Trabajo, cofre: Cofre) {
        if (trabajo.estado != "completado") return
        val fichero = ficheroDe(carpeta, trabajo.id) ?: return
        carpeta.mkdirs()
        fichero.writeText(cofre.cifrar(aJson(trabajo).toString()))
    }

    fun cargarDe(carpeta: File, idTrabajo: String, cofre: Cofre): Trabajo? {
        val fichero = ficheroDe(carpeta, idTrabajo) ?: return null
        if (!fichero.exists()) return null
        return leer(fichero, cofre)
    }

    /** De la más reciente a la más antigua, igual que el listado del servidor. */
    fun listarDe(carpeta: File, cofre: Cofre): List<Trabajo> =
        (carpeta.listFiles()?.toList() ?: emptyList())
            .filter { it.extension == "json" }
            .mapNotNull { leer(it, cofre) }
            .sortedByDescending { it.creado.orEmpty() }

    fun borrarDe(carpeta: File, idTrabajo: String) {
        ficheroDe(carpeta, idTrabajo)?.delete()
    }

    fun vaciarEn(carpeta: File) {
        carpeta.deleteRecursively()
    }

    /** `null` si el id no sirve para nombrar un fichero dentro de la carpeta. */
    private fun ficheroDe(carpeta: File, idTrabajo: String): File? =
        if (ID_VALIDO.matches(idTrabajo)) File(carpeta, "$idTrabajo.json") else null

    /**
     * Un JSON a medias —la batería se agotó al escribir— no puede dejar sin
     * historial a todo lo demás: esa conversación se pierde, el resto no. Lo
     * mismo vale para una que ya no se pueda descifrar.
     *
     * Si la escribió una versión anterior, que guardaba en claro, se lee y se
     * reescribe cifrada aquí mismo: si no, solo se cifraría lo nuevo y el
     * historial de antes se quedaría al descubierto para siempre.
     */
    private fun leer(fichero: File, cofre: Cofre): Trabajo? = try {
        val guardado = fichero.readText()
        val json = cofre.leerAunqueSeaViejo(guardado)
        when {
            json == null -> null
            else -> {
                if (!estaCifrado(guardado)) fichero.writeText(cofre.cifrar(json))
                deJson(JSONObject(json))
            }
        }
    } catch (_: Exception) {
        null
    }

    private fun aJson(trabajo: Trabajo): JSONObject = JSONObject().apply {
        put("id", trabajo.id)
        put("estado", trabajo.estado)
        put("creado", trabajo.creado)
        put("titulo", trabajo.titulo)
        put("hablantes", trabajo.hablantes)
        put("etiquetas", JSONArray(trabajo.etiquetas))
        put("nombres", JSONObject(trabajo.nombres.toMap()))
    }

    private fun deJson(objeto: JSONObject): Trabajo {
        val etiquetas = objeto.optJSONArray("etiquetas")?.let { lista ->
            (0 until lista.length()).map { lista.getString(it) }
        } ?: emptyList()
        val nombres = objeto.optJSONObject("nombres")?.let { mapa ->
            mapa.keys().asSequence().associateWith { clave -> mapa.getString(clave) }
        } ?: emptyMap()
        return Trabajo(
            id = objeto.getString("id"),
            estado = objeto.getString("estado"),
            creado = textoONulo(objeto, "creado"),
            titulo = textoONulo(objeto, "titulo"),
            hablantes = textoONulo(objeto, "hablantes"),
            etiquetas = etiquetas,
            nombres = nombres,
        )
    }

    private fun textoONulo(objeto: JSONObject, clave: String): String? =
        if (!objeto.has(clave) || objeto.isNull(clave)) null else objeto.getString(clave)
}
