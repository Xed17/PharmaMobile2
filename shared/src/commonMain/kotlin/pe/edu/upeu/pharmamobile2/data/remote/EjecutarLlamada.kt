package pe.edu.upeu.pharmamobile2.data.remote

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile2.data.remote.dto.ErrorResponseDto
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApi
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException

suspend fun <T> ejecutarLlamada(
    bloque: suspend () -> T
): Result<T> =
    try {
        Result.success(bloque())
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (e: ClientRequestException) {
        Result.failure(
            ErrorApiException(traducirCliente(e))
        )
    } catch (e: ServerResponseException) {
        Result.failure(
            ErrorApiException(ErrorApi.Servidor)
        )
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(
            ErrorApiException(ErrorApi.TiempoAgotado)
        )
    } catch (e: pe.edu.upeu.pharmamobile2.domain.model.FalloOperacionException) {
        Result.failure(e)
    } catch (e: io.ktor.serialization.ContentConvertException) {
        Result.failure(ErrorApiException(ErrorApi.DatosInvalidos))
    } catch (e: kotlinx.serialization.SerializationException) {
        Result.failure(ErrorApiException(ErrorApi.DatosInvalidos))
    } catch (e: io.ktor.client.call.NoTransformationFoundException) {
        Result.failure(ErrorApiException(ErrorApi.DatosInvalidos))
    } catch (e: io.ktor.utils.io.errors.IOException) {
        Result.failure(ErrorApiException(ErrorApi.SinConexion))
    } catch (e: Exception) {
        Result.failure(ErrorApiException(ErrorApi.Inesperado))
    }

private suspend fun traducirCliente(
    e: ClientRequestException
): ErrorApi {
    val cuerpo = runCatching {
        e.response.body<ErrorResponseDto>()
    }.getOrNull()

    return when (e.response.status.value) {
        400 -> ErrorApi.Validacion(
            cuerpo?.validationErrors.orEmpty()
        )

        401 -> ErrorApi.NoAutorizado

        403 -> ErrorApi.SinPermisos(
            cuerpo?.message ?: "No tienes permiso para esta operación"
        )

        404 -> ErrorApi.NoEncontrado

        409 -> ErrorApi.Conflicto(
            cuerpo?.message ?: "Operación no permitida"
        )

        else -> ErrorApi.Servidor
    }
}
