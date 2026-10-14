package pe.edu.upeu.pharmamobile2.domain.error

sealed interface ErrorApi {
    data class Validacion(
        val porCampo: Map<String, String>
    ) : ErrorApi

    data object NoEncontrado : ErrorApi
    data class Conflicto(val mensaje: String) : ErrorApi
    data object NoAutorizado : ErrorApi
    data class SinPermisos(val mensaje: String = "No tienes permiso para esta operación") : ErrorApi
    data object Servidor : ErrorApi
    data object SinConexion : ErrorApi
    data object TiempoAgotado : ErrorApi
}

class ErrorApiException(
    val error: ErrorApi
) : Exception()
