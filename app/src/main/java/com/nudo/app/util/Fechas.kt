package com.nudo.app.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Fechas cortas en mono, como en la identidad: "16 jul · 20:14". */
object Fechas {

    private val CASTELLANO = Locale.forLanguageTag("es-ES")
    private val FORMATO_CORTO = DateTimeFormatter.ofPattern("d MMM · HH:mm", CASTELLANO)
    private val FORMATO_HOY = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", CASTELLANO)

    fun formatearCreado(isoUtc: String, zona: ZoneId = ZoneId.systemDefault()): String =
        try {
            OffsetDateTime.parse(isoUtc).atZoneSameInstant(zona).format(FORMATO_CORTO)
                .replace(".", "")
        } catch (_: Exception) {
            isoUtc
        }

    fun formatearInstante(milisegundos: Long, zona: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(milisegundos).atZone(zona).format(FORMATO_CORTO).replace(".", "")

    fun hoy(zona: ZoneId = ZoneId.systemDefault()): String =
        Instant.now().atZone(zona).format(FORMATO_HOY)
}
