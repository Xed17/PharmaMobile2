package pe.edu.upeu.pharmamobile2.domain.repository

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.pharmamobile2.domain.model.Credenciales
import pe.edu.upeu.pharmamobile2.domain.model.Sesion
import pe.edu.upeu.pharmamobile2.domain.model.AccesoSesion
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta

interface AuthRepository {
    val acceso: StateFlow<AccesoSesion>
    suspend fun habilitarContinuidadLocal(): Result<ContextoCuenta>
    suspend fun sesionRemota(contexto: ContextoCuenta): Sesion?
    fun esActual(contexto: ContextoCuenta): Boolean

    val sesionActual: StateFlow<Sesion?>
    val evento401: SharedFlow<Unit>
    val evento403: SharedFlow<String>

    suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion>
    suspend fun recuperarSesion(): Result<Sesion?>
    suspend fun cerrarSesion()

    fun notificar401()
    fun notificar403(mensaje: String = "No tienes permiso para esta operación")
}
