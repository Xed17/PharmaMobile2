package pe.edu.upeu.pharmamobile2.domain.model

sealed interface ErrorSesion {
    val mensaje: String

    data class CredencialesIncorrectas(
        override val mensaje: String = "Usuario o contraseña incorrectos"
    ) : ErrorSesion

    data class SesionExpirada(
        override val mensaje: String = "Su sesión ha expirado. Por favor inicie sesión nuevamente."
    ) : ErrorSesion

    data class SinPermisos(
        override val mensaje: String = "No tienes permiso para esta operación"
    ) : ErrorSesion

    data class SinConexion(
        override val mensaje: String = "No fue posible conectar con el servidor. Verifique su red."
    ) : ErrorSesion

    data class Servidor(
        override val mensaje: String
    ) : ErrorSesion
}

class SesionException(val error: ErrorSesion) : Exception(error.mensaje)
