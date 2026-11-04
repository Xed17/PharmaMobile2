package pe.edu.upeu.pharmamobile2.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.pharmamobile2.domain.model.Credenciales
import pe.edu.upeu.pharmamobile2.domain.model.Sesion

interface AuthRepository {
    val acceso: StateFlow<pe.edu.upeu.pharmamobile2.domain.model.AccesoSesion>
    suspend fun habilitarContinuidadLocal(): Result<pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta>
    suspend fun sesionRemota(contexto: pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta): Sesion?
    fun esActual(contexto: pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta): Boolean

    val sesionActual: StateFlow<Sesion?>
    val evento401: SharedFlow<Unit>
    val evento403: SharedFlow<String>

    suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion>
    suspend fun recuperarSesion(): Result<Sesion?>
    suspend fun cerrarSesion()

    fun notificar401()
    fun notificar403(mensaje: String = "No tienes permiso para esta operaciÃ³n")
}
