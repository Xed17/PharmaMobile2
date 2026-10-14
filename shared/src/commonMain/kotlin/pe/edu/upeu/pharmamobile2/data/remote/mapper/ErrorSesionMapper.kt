package pe.edu.upeu.pharmamobile2.data.remote.mapper

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile2.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobile2.domain.model.ErrorSesion

suspend fun Throwable.toErrorSesion(esLogin: Boolean = false): ErrorSesion = when (this) {
    is ClientRequestException -> {
        val cuerpo = runCatching { response.body<ErrorResponseDto>() }.getOrNull()
        val mensajeBackend = cuerpo?.message
        when (response.status.value) {
            401 -> if (esLogin) {
                ErrorSesion.CredencialesIncorrectas(
                    mensajeBackend ?: "Usuario o contraseña incorrectos"
                )
            } else {
                ErrorSesion.SesionExpirada(
                    mensajeBackend ?: "Su sesión ha expirado. Por favor inicie sesión nuevamente."
                )
            }
            403 -> ErrorSesion.SinPermisos(
                mensajeBackend ?: "No tienes permiso para esta operación"
            )
            400 -> ErrorSesion.Servidor(
                mensajeBackend ?: "Solicitud inválida"
            )
            else -> ErrorSesion.Servidor(
                mensajeBackend ?: "Error en la solicitud (${response.status.value})"
            )
        }
    }
    is ServerResponseException -> ErrorSesion.Servidor(
        "Error interno del servidor (${response.status.value})"
    )
    is HttpRequestTimeoutException -> ErrorSesion.SinConexion(
        "Tiempo de espera agotado al conectar con el servidor"
    )
    is CancellationException -> throw this
    else -> ErrorSesion.SinConexion()
}
