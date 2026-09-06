package com.nudo.app.almacen

/**
 * Marca al principio de todo lo que un cofre ha cifrado.
 *
 * Sirve para distinguir lo cifrado de lo que dejó una versión anterior, que
 * guardaba en claro. Sin ella, actualizar la app dejaría el historial y la
 * credencial ilegibles: se intentaría descifrar algo que nunca se cifró.
 */
const val MARCA_CIFRADO = "nudo1:"

/**
 * Cifra y descifra lo que se queda guardado en este móvil.
 *
 * Es una interfaz y no un objeto porque el cofre de verdad ([CofreKeystore])
 * necesita el Keystore de Android, que no existe en la JVM: así los almacenes
 * que lo usan se pueden probar sin un emulador.
 */
interface Cofre {

    /** Devuelve [MARCA_CIFRADO] seguido del texto cifrado. */
    fun cifrar(claro: String): String

    /** `null` si no se puede descifrar: clave perdida o guardado de otra instalación. */
    fun descifrar(guardado: String): String?
}

fun estaCifrado(guardado: String): Boolean = guardado.startsWith(MARCA_CIFRADO)

/**
 * Lee un valor que puede venir cifrado (lo normal) o en claro (lo que dejó una
 * versión anterior a esta). Quien llame se encarga de reescribirlo cifrado.
 */
fun Cofre.leerAunqueSeaViejo(guardado: String): String? =
    if (estaCifrado(guardado)) descifrar(guardado) else guardado
