package com.nudo.app.red

import com.nudo.app.BuildConfig
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val TIPO_JSON = "application/json; charset=utf-8".toMediaType()

private const val CODIGO_NO_AUTORIZADO = 401

/** El servidor no reconoce la credencial: hay que pedir una nueva. */
private class CredencialRechazada : IOException("Credencial rechazada")

/** El servidor valida el formato por la extensión del nombre que le mandamos. */
private val TIPOS_POR_EXTENSION = mapOf(
    "m4a" to "audio/mp4",
    "mp3" to "audio/mpeg",
    "wav" to "audio/wav",
    "ogg" to "audio/ogg",
    "opus" to "audio/opus",
    "aac" to "audio/aac",
    "flac" to "audio/flac",
    "webm" to "audio/webm",
)

data class TrabajoResumen(
    val id: String,
    val estado: String,
    val creado: String,
    val titulo: String?,
)

/**
 * Una conversación. La transcripción llega en [hablantes] ya con los nombres
 * puestos; [etiquetas] son las originales del diarizador (SPEAKER_00...) y
 * [nombres] el alias que el usuario ha dado a cada una.
 */
data class Trabajo(
    val id: String,
    val estado: String,
    val creado: String? = null,
    val titulo: String? = null,
    val hablantes: String? = null,
    val etiquetas: List<String> = emptyList(),
    val nombres: Map<String, String> = emptyMap(),
    val error: String? = null,
)

object ClienteNudo {

    private val candadoRegistro = Any()

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    fun subirAudio(archivo: File): String {
        val tipo = TIPOS_POR_EXTENSION[archivo.extension.lowercase()] ?: "application/octet-stream"
        return parsearId(
            autenticada("/trabajos") { peticion ->
                val cuerpo = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("audio", archivo.name, archivo.asRequestBody(tipo.toMediaType()))
                    .build()
                peticion.post(cuerpo)
            },
        )
    }

    fun listarTrabajos(): List<TrabajoResumen> =
        parsearLista(autenticada("/trabajos") { it.get() })

    fun consultarTrabajo(idTrabajo: String): Trabajo =
        parsearTrabajo(autenticada("/trabajos/$idTrabajo") { it.get() })

    /**
     * Renombra hablantes de golpe. Poner "Diego" en SPEAKER_00 cambia todas sus
     * intervenciones; un nombre vacío devuelve la etiqueta original.
     */
    fun renombrarHablantes(idTrabajo: String, nombres: Map<String, String>): Trabajo {
        val cuerpo = JSONObject().put("nombres", JSONObject(nombres.toMap()))
        return parsearTrabajo(
            autenticada("/trabajos/$idTrabajo/hablantes") {
                it.put(cuerpo.toString().toRequestBody(TIPO_JSON))
            },
        )
    }

    fun cambiarTitulo(idTrabajo: String, titulo: String): String {
        val cuerpo = JSONObject().put("titulo", titulo)
        val respuesta = autenticada("/trabajos/$idTrabajo/titulo") {
            it.put(cuerpo.toString().toRequestBody(TIPO_JSON))
        }
        return JSONObject(respuesta).getString("titulo")
    }

    /**
     * Lanza la petición con la credencial de este dispositivo. Si el servidor la
     * rechaza, la olvida, pide una nueva y reintenta **una sola vez**: eso cubre
     * que el token se haya revocado desde otro sitio, sin arriesgar un bucle.
     */
    private fun autenticada(ruta: String, preparar: (Request.Builder) -> Request.Builder): String {
        try {
            return ejecutar(preparar(peticionBase(ruta, credencial())).build())
        } catch (rechazo: CredencialRechazada) {
            AlmacenCredencial.olvidar()
        }
        return ejecutar(preparar(peticionBase(ruta, credencial())).build())
    }

    /**
     * El token propio, pidiéndolo al servidor la primera vez. Solo desde hilo de red.
     *
     * El candado no sobra: al abrir la app salen varias peticiones a la vez
     * (historial y sondeo, al menos). Sin él, todas veían el almacén vacío a la vez
     * y cada una daba de alta su propio dispositivo. La primera versión registró
     * dos con un milisegundo de diferencia.
     */
    private fun credencial(): String {
        AlmacenCredencial.token()?.let { return it }
        synchronized(candadoRegistro) {
            // Se vuelve a mirar ya dentro: otra petición pudo registrarlo mientras
            // esta esperaba en la puerta.
            AlmacenCredencial.token()?.let { return it }
            return registrarDispositivo()
        }
    }

    /**
     * Da de alta esta instalación y guarda su token. La clave de arranque del
     * BuildConfig solo sirve para esto: no da acceso a ninguna conversación.
     */
    private fun registrarDispositivo(): String {
        val cuerpo = JSONObject().put("nombre", AlmacenCredencial.nombreDeEsteDispositivo())
        val peticion = Request.Builder()
            .url(BuildConfig.NUDO_BASE_URL + "/dispositivos")
            .header("X-API-Key", BuildConfig.NUDO_CLAVE_ARRANQUE)
            .post(cuerpo.toString().toRequestBody(TIPO_JSON))
            .build()
        val respuesta = JSONObject(ejecutar(peticion))
        val token = respuesta.getString("token")
        AlmacenCredencial.guardar(respuesta.getString("id"), token)
        return token
    }

    private fun ejecutar(peticion: Request): String {
        cliente.newCall(peticion).execute().use { respuesta ->
            val texto = respuesta.body?.string().orEmpty()
            if (respuesta.code == CODIGO_NO_AUTORIZADO) {
                throw CredencialRechazada()
            }
            if (!respuesta.isSuccessful) {
                throw IOException("El servidor respondió ${respuesta.code}: $texto")
            }
            return texto
        }
    }

    private fun peticionBase(ruta: String, credencial: String): Request.Builder =
        Request.Builder()
            .url(BuildConfig.NUDO_BASE_URL + ruta)
            .header("X-API-Key", credencial)

    // ---- Parseo (público para poder probarlo sin red) ----

    fun parsearId(json: String): String = JSONObject(json).getString("id")

    fun parsearLista(json: String): List<TrabajoResumen> {
        val lista = JSONObject(json).getJSONArray("trabajos")
        return (0 until lista.length()).map { indice ->
            val objeto = lista.getJSONObject(indice)
            TrabajoResumen(
                id = objeto.getString("id"),
                estado = objeto.getString("estado"),
                creado = objeto.getString("creado"),
                titulo = textoONulo(objeto, "titulo"),
            )
        }
    }

    fun parsearTrabajo(json: String): Trabajo {
        val objeto = JSONObject(json)
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
            error = textoONulo(objeto, "error"),
        )
    }

    /** optString devuelve la cadena "null" para valores JSON null; esto lo evita. */
    private fun textoONulo(objeto: JSONObject, clave: String): String? =
        if (!objeto.has(clave) || objeto.isNull(clave)) null else objeto.getString(clave)
}
