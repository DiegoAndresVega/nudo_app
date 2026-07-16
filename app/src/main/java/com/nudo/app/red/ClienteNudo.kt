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

data class TrabajoResumen(
    val id: String,
    val estado: String,
    val creado: String,
    val titulo: String?,
)

data class Trabajo(
    val id: String,
    val estado: String,
    val creado: String? = null,
    val titulo: String? = null,
    val hablantes: String? = null,
    val analisis: Map<String, String> = emptyMap(),
    val error: String? = null,
)

object ClienteNudo {

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    fun subirAudio(archivo: File): String {
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", archivo.name, archivo.asRequestBody("audio/mp4".toMediaType()))
            .build()
        return parsearId(ejecutar(peticionBase("/trabajos").post(cuerpo).build()))
    }

    fun listarTrabajos(): List<TrabajoResumen> =
        parsearLista(ejecutar(peticionBase("/trabajos").get().build()))

    fun consultarTrabajo(idTrabajo: String): Trabajo =
        parsearTrabajo(ejecutar(peticionBase("/trabajos/$idTrabajo").get().build()))

    fun pedirAnalisis(idTrabajo: String, tipo: String): String {
        val peticion = peticionBase("/trabajos/$idTrabajo/analisis/$tipo")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        return parsearAnalisis(ejecutar(peticion))
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
        val analisis = objeto.optJSONObject("analisis")?.let { mapa ->
            mapa.keys().asSequence().associateWith { clave -> mapa.getString(clave) }
        } ?: emptyMap()
        return Trabajo(
            id = objeto.getString("id"),
            estado = objeto.getString("estado"),
            creado = textoONulo(objeto, "creado"),
            titulo = textoONulo(objeto, "titulo"),
            hablantes = textoONulo(objeto, "hablantes"),
            analisis = analisis,
            error = textoONulo(objeto, "error"),
        )
    }

    fun parsearAnalisis(json: String): String = JSONObject(json).getString("contenido")

    /** optString devuelve la cadena "null" para valores JSON null; esto lo evita. */
    private fun textoONulo(objeto: JSONObject, clave: String): String? =
        if (!objeto.has(clave) || objeto.isNull(clave)) null else objeto.getString(clave)
}
