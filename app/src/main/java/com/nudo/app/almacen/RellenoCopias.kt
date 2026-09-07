package com.nudo.app.almacen

import com.nudo.app.red.Trabajo
import com.nudo.app.red.TrabajoResumen

/**
 * Completa la copia local con lo que está en el servidor y no en el móvil (#29).
 *
 * La copia de #18 se escribía **al abrir** una conversación, así que protegía lo
 * que el usuario hubiera mirado y no lo que tuviera: de 6 conversaciones en el
 * servidor, en modo avión se veía 1. Con eso, perder el volumen del VPS no era
 * «perderlo todo» sino «perder todo lo que no hayas abierto», que es peor porque
 * suena a que hay copia.
 *
 * Se cruzaba además mal con la caducidad de #24: una conversación que nadie abre
 * en 90 días se va del servidor sin haber dejado copia en ninguna parte.
 *
 * Es una función suelta y sin Android para poder probarla en la JVM; quien la
 * llama pone el cliente y el almacén de verdad.
 */
object RellenoCopias {

    /**
     * Pide y guarda las que falten. Devuelve cuántas copió.
     *
     * De una en una y en orden, nunca en paralelo: el VPS es de 1 vCPU y hay
     * límite de tasa por credencial (#6). Quien tenga cien conversaciones no
     * puede lanzar cien peticiones a la vez, ni ganarse un 429 por intentar
     * salvaguardar lo suyo.
     *
     * Sin red, [resumenes] llega vacía —quien llama ya no pudo listar— y esto no
     * intenta nada ni avisa de nada: abrir la app en avión es normal.
     */
    fun rellenar(
        resumenes: List<TrabajoResumen>,
        yaCopiadas: Set<String>,
        traer: (String) -> Trabajo,
        guardar: (Trabajo) -> Unit,
    ): Int {
        var copiadas = 0
        for (resumen in resumenes) {
            if (resumen.estado != ESTADO_TERMINADO || resumen.id in yaCopiadas) continue
            try {
                guardar(traer(resumen.id))
                copiadas++
            } catch (_: Exception) {
                // Una que no se pueda traer no puede dejar sin copia a las que
                // vienen detrás. Se salta y se reintenta en la siguiente carga
                // del historial, que es cuando se vuelve a pasar por aquí.
                continue
            }
        }
        return copiadas
    }

    /**
     * Solo las terminadas. Una a medias no tiene nada que conservar, y copiarla
     * haría creer que hay copia de algo que todavía no existe. Mismo criterio que
     * [CopiaLocal.guardarEn], que además lo vuelve a comprobar por su cuenta.
     */
    private const val ESTADO_TERMINADO = "completado"
}
