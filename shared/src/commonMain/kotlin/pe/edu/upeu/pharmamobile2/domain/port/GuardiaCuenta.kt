package pe.edu.upeu.pharmamobile2.domain.port

import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta

interface GuardiaCuenta {
    fun esActual(contexto: ContextoCuenta): Boolean
    suspend fun <T> escribirSiVigente(contexto: ContextoCuenta, bloque: () -> T): T
}
