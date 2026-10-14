package pe.edu.upeu.pharmamobile2.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile2.data.remote.dto.LoginRequestDto
import pe.edu.upeu.pharmamobile2.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobile2.data.remote.dto.UsuarioActualDto
import pe.edu.upeu.pharmamobile2.data.remote.limpiarTokensAuth
import pe.edu.upeu.pharmamobile2.data.remote.mapper.toDomain
import pe.edu.upeu.pharmamobile2.data.remote.mapper.toErrorSesion
import pe.edu.upeu.pharmamobile2.domain.model.Credenciales
import pe.edu.upeu.pharmamobile2.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobile2.domain.model.Sesion
import pe.edu.upeu.pharmamobile2.domain.model.SesionException
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository

class AuthRepositorioRest(
    private val client: HttpClient,
    private val almacenSeguro: AlmacenSeguro
) : AuthRepository {

    private val _sesionActual = MutableStateFlow<Sesion?>(null)
    override val sesionActual: StateFlow<Sesion?> = _sesionActual.asStateFlow()

    private val _evento401 = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val evento401: SharedFlow<Unit> = _evento401.asSharedFlow()

    private val _evento403 = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val evento403: SharedFlow<String> = _evento403.asSharedFlow()

    override suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion> {
        if (credenciales.username.isBlank() || credenciales.clave.isBlank()) {
            return Result.failure(
                SesionException(ErrorSesion.CredencialesIncorrectas("Usuario y contraseña son requeridos"))
            )
        }

        return runCatching {
            val response: LoginResponseDto = client.post("api/v1/auth/login") {
                setBody(
                    LoginRequestDto(
                        username = credenciales.username.trim(),
                        password = credenciales.clave
                    )
                )
            }.body()

            if (response.token.isBlank()) {
                throw SesionException(ErrorSesion.Servidor("El servidor no devolvió un token de acceso válido"))
            }

            almacenSeguro.guardarToken(response.token)
            client.limpiarTokensAuth()

            val sesion = response.toDomain()
            _sesionActual.value = sesion
            sesion
        }.recoverCatching { throwable ->
            val errorSesion = throwable.toErrorSesion(esLogin = true)
            throw SesionException(errorSesion)
        }
    }

    override suspend fun recuperarSesion(): Result<Sesion?> {
        val token = almacenSeguro.obtenerToken()
        if (token.isNullOrBlank()) {
            _sesionActual.value = null
            return Result.success(null)
        }

        return runCatching {
            val usuarioActual: UsuarioActualDto = client.get("api/v1/auth/me").body()
            val sesion = usuarioActual.toDomain(token)
            _sesionActual.value = sesion
            sesion
        }.recoverCatching { throwable ->
            if (throwable is ClientRequestException && throwable.response.status.value == 401) {
                // Token expirado o inválido: limpiar almacenamiento y caché
                almacenSeguro.eliminarToken()
                client.limpiarTokensAuth()
                _sesionActual.value = null
                notificar401()
                throw SesionException(ErrorSesion.SesionExpirada())
            }

            val errorSesion = throwable.toErrorSesion(esLogin = false)
            throw SesionException(errorSesion)
        }
    }

    override suspend fun cerrarSesion() {
        almacenSeguro.eliminarToken()
        client.limpiarTokensAuth()
        _sesionActual.value = null
    }

    override fun notificar401() {
        _sesionActual.value = null
        _evento401.tryEmit(Unit)
    }

    override fun notificar403(mensaje: String) {
        _evento403.tryEmit(mensaje)
    }
}
