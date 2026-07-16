package com.nudo.app.grabacion

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

private const val BITRATE_AUDIO = 128_000
private const val FRECUENCIA_MUESTREO = 44_100

class GrabadoraAudio {

    private var grabadora: MediaRecorder? = null

    fun iniciar(contexto: Context, archivoSalida: File) {
        val nueva = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(contexto)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        nueva.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(BITRATE_AUDIO)
            setAudioSamplingRate(FRECUENCIA_MUESTREO)
            setAudioChannels(1)
            setOutputFile(archivoSalida.absolutePath)
            prepare()
            start()
        }
        grabadora = nueva
    }

    /** Amplitud máxima desde la última llamada (0..32767); 0 si no se está grabando. */
    fun amplitudMaxima(): Int =
        try {
            grabadora?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }

    fun detener() {
        grabadora?.apply {
            stop()
            release()
        }
        grabadora = null
    }

    fun cancelar() {
        grabadora?.release()
        grabadora = null
    }
}
