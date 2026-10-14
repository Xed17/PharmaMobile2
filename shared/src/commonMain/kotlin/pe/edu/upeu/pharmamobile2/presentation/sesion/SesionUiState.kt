package pe.edu.upeu.pharmamobile2.presentation.sesion

import pe.edu.upeu.pharmamobile2.domain.model.Sesion

sealed interface EstadoSesion {
    data object Comprobando : EstadoSesion
    data class SinSesion(val aviso: String? = null) : EstadoSesion
    data class Autenticada(val sesion: Sesion) : EstadoSesion
    data class ErrorRecuperacion(val mensaje: String) : EstadoSesion
}

data class SesionUiState(
    val estado: EstadoSesion = EstadoSesion.Comprobando,
    val usernameInput: String = "",
    val claveInput: String = "",
    val claveVisible: Boolean = false,
    val usernameError: String? = null,
    val claveError: String? = null,
    val estaCargando: Boolean = false,
    val errorLogin: String? = null
)
