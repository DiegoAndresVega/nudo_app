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

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    fun subirAudio(archivo: File): String {
        val tipo = TIPOS_POR_EXTENSION[archivo.extension.lowercase()] ?: "application/octet-stream"
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", archivo.name, archivo.asRequestBody(tipo.toMediaType()))
            .build()
        return parsearId(ejecutar(peticionBase("/trabajos").post(cuerpo).build()))
    }

    fun listarTrabajos(): List<TrabajoResumen> =
        parsearLista(ejecutar(peticionBase("/trabajos").get().build()))

    fun consultarTrabajo(idTrabajo: String): Trabajo =
        parsearTrabajo(ejecutar(peticionBase("/trabajos/$idTrabajo").get().build()))

    /**
     * Renombra hablantes de golpe. Poner "Diego" en SPEAKER_00 cambia todas sus
     * intervenciones; un nombre vacío devuelve la etiqueta original.
     */
    fun renombrarHablantes(idTrabajo: String, nombres: Map<String, String>): Trabajo {
        val cuerpo = JSONObject().put("nombres", JSONObject(nombres.toMap()))
        val peticion = peticionBase("/trabajos/$idTrabajo/hablantes")
            .put(cuerpo.toString().toRequestBody(TIPO_JSON))
            .build()
        return parsearTrabajo(ejecutar(peticion))
    }

    fun cambiarTitulo(idTrabajo: String, titulo: String): String {
        val cuerpo = JSONObject().put("titulo", titulo)
        val peticion = peticionBase("/trabajos/$idTrabajo/titulo")
            .put(cuerpo.toString().toRequestBody(TIPO_JSON))
            .build()
        return JSONObject(ejecutar(peticion)).getString("titulo")
    }

    private fun ejecutar(peticion: Request): String {
        cliente.newCall(peticion).execute().use { respuesta ->
            val texto = respuesta.body?.string().orEmpty()
            if (!respuesta.isSuccessful) {
                throw IOException("El servidor respondió ${respuesta.code}: $texto")
            }
            return texto
        }
    }

    private fun peticionBase(ruta: String): Request.Builder =
        Request.Builder()
            .url(BuildConfig.NUDO_BASE_URL + ruta)
            .header("X-API-Key", BuildConfig.NUDO_API_KEY)

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
