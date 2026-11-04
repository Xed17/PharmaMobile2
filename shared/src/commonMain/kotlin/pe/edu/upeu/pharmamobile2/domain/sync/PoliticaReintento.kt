package pe.edu.upeu.pharmamobile2.domain.sync

import pe.edu.upeu.pharmamobile2.domain.model.*

/** A transient failure alone does not make repeating a remote effect safe. */
class PoliticaReintento {
    fun esperaMs(fallosPrevios: Int): Long {
        require(fallosPrevios >= 0) { "El contador de fallos no puede ser negativo" }
        return 1000L shl fallosPrevios.coerceAtMost(4)
    }
    fun puedeReintentarAutomaticamente(error: FalloOperacion, esLectura: Boolean): Boolean =
        esLectura && !error.efectoIncierto && error.naturaleza == NaturalezaFallo.TRANSITORIO

    fun puedeIniciarEnvio(estado: EstadoEnvio, intentos: Int): Boolean {
        require(intentos >= 0) { "El contador de intentos no puede ser negativo" }
        return estado == EstadoEnvio.PENDIENTE && intentos < MAX_INTENTOS
    }
    companion object { const val MAX_INTENTOS = 5 }
}
