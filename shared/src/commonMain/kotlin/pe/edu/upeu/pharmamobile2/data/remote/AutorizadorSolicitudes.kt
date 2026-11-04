package pe.edu.upeu.pharmamobile2.data.remote

import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.error.*
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository

class AutorizadorSolicitudes(private val auth: AuthRepository) {
    suspend fun <T> conCredencial(contexto: ContextoCuenta, bloque: suspend (String) -> T): T {
        if(!auth.esActual(contexto)) throw CancellationException("Contexto de cuenta obsoleto")
        val sesion = auth.sesionRemota(contexto) ?: throw ErrorApiException(ErrorApi.NoAutorizado)
        val resultado = bloque(sesion.token)
        if(!auth.esActual(contexto)) throw CancellationException("La cuenta cambió durante la solicitud")
        return resultado
    }
}
