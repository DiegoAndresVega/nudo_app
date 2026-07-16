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
import org.json.JSONObject

data class Trabajo(
    val id: String,
    val estado: String,
    val notas: String? = null,
    val hablantes: String? = null,
    val error: String? = null,
)

object ClienteNudo {

    private val cliente = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    fun subirAudio(archivo: File): String {
        val cuerpo = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", archivo.name, archivo.asRequestBody("audio/mp4".toMediaType()))
            .build()
        val peticion = peticionBase("/trabajos").post(cuerpo).build()
        cliente.newCall(peticion).execute().use { respuesta ->
            val texto = respuesta.body?.string().orEmpty()
            if (!respuesta.isSuccessful) {
                throw IOException("El servidor respondió ${respuesta.code}: $texto")
            }
            return parsearId(texto)
        }
    }

    fun consultarTrabajo(idTrabajo: String): Trabajo {
        val peticion = peticionBase("/trabajos/$idTrabajo").get().build()
        cliente.newCall(peticion).execute().use { respuesta ->
            val texto = respuesta.body?.string().orEmpty()
            if (!respuesta.isSuccessful) {
                throw IOException("El servidor respondió ${respuesta.code}: $texto")
            }
            return parsearTrabajo(texto)
        }
    }

    private fun peticionBase(ruta: String): Request.Builder =
        Request.Builder()
            .url(BuildConfig.NUDO_BASE_URL + ruta)
            .header("X-API-Key", BuildConfig.NUDO_API_KEY)

    fun parsearId(json: String): String = JSONObject(json).getString("id")

    fun parsearTrabajo(json: String): Trabajo {
        val objeto = JSONObject(json)
        return Trabajo(
            id = objeto.getString("id"),
            estado = objeto.getString("estado"),
            notas = objeto.optString("notas").ifEmpty { null },
            hablantes = objeto.optString("hablantes").ifEmpty { null },
            error = objeto.optString("error").ifEmpty { null },
        )
    }
}
