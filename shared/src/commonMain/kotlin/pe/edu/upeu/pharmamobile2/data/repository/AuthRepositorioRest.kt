package pe.edu.upeu.pharmamobile2.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile2.data.remote.dto.*
import pe.edu.upeu.pharmamobile2.data.remote.limpiarTokensAuth
import pe.edu.upeu.pharmamobile2.data.remote.mapper.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.Reloj
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile2.platform.obtenerTiempoActualMs

class AuthRepositorioRest(
    private val client: HttpClient,
    private val almacenSeguro: AlmacenSeguro,
    private val reloj: Reloj = object : Reloj { override fun ahoraMs() = obtenerTiempoActualMs() },
    private val entorno: String = "pharmasoft-desarrollo-local"
) : AuthRepository {
    private val _sesionActual = MutableStateFlow<Sesion?>(null)
    override val sesionActual = _sesionActual.asStateFlow()
    private val _acceso = MutableStateFlow<AccesoSesion>(AccesoSesion.SinSesion)
    override val acceso = _acceso.asStateFlow()
    private val _evento401 = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val evento401 = _evento401.asSharedFlow()
    private val _evento403 = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val evento403 = _evento403.asSharedFlow()
    private var generacion = 0L
    private var perfilActual: PerfilLocal? = null

    private fun contexto(username: String) = ContextoCuenta(
        "v1|${entorno.length}:$entorno|${username.length}:$username", ++generacion
    )
    private fun contextoActual(): ContextoCuenta? = when(val estado = _acceso.value) {
        is AccesoSesion.Disponible -> estado.contexto
        is AccesoSesion.Reautenticar -> estado.contexto
        AccesoSesion.SinSesion -> null
    }
    override fun esActual(contexto: ContextoCuenta) = contextoActual() == contexto
    override suspend fun sesionRemota(contexto: ContextoCuenta): Sesion? {
        val estado = _acceso.value as? AccesoSesion.Disponible ?: return null
        val perfil = perfilActual ?: return null
        val ahora = reloj.ahoraMs()
        if (estado.contexto != contexto || estado.modo != ModoAcceso.REMOTO ||
            ahora < perfil.validadoEnMs || perfil.expiraTokenEnMs?.let { ahora >= it } == true) return null
        return _sesionActual.value
    }
    override suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion> {
        if (credenciales.username.isBlank() || credenciales.clave.isBlank()) return Result.failure(
            SesionException(ErrorSesion.CredencialesIncorrectas("Usuario y contraseña son requeridos")))
        return try {
            val respuesta: LoginResponseDto = client.post("api/v1/auth/login") {
                setBody(LoginRequestDto(credenciales.username.trim(), credenciales.clave))
            }.body()
            require(respuesta.token.isNotBlank() && respuesta.username.isNotBlank()) { "Respuesta de sesión inválida" }
            val sesion = respuesta.toDomain()
            val ahora = reloj.ahoraMs()
            val ttl = respuesta.expiraEnSegundos.coerceIn(0, 31_536_000) * 1000L
            val perfil = PerfilLocal(sesion.username, sesion.nombreCompleto, sesion.roles,
                sesion.permisos, ahora, ahora + ttl)
            try {
                almacenSeguro.guardarToken(sesion.token)
                almacenSeguro.guardarPerfil(perfil)
            } catch (e: Exception) {
                almacenSeguro.eliminarToken()
                almacenSeguro.borrarContinuidad()
                throw e
            }
            client.limpiarTokensAuth()
            activar(sesion, perfil, ModoAcceso.REMOTO)
            Result.success(sesion)
        } catch (e: CancellationException) { throw e
        } catch (e: Throwable) { Result.failure(SesionException(e.toErrorSesion(esLogin = true))) }
    }
    private fun activar(sesion: Sesion, perfil: PerfilLocal, modo: ModoAcceso) {
        perfilActual = perfil
        _sesionActual.value = sesion
        _acceso.value = AccesoSesion.Disponible(contexto(perfil.username), perfil, modo)
    }
    override suspend fun habilitarContinuidadLocal(): Result<ContextoCuenta> = try {
        val perfil = almacenSeguro.obtenerPerfil() ?: error("Inicia sesión con conexión antes de continuar sin red")
        activar(Sesion("", username = perfil.username, nombreCompleto = perfil.nombreCompleto,
            roles = perfil.roles, permisos = perfil.permisos), perfil, ModoAcceso.LOCAL)
        Result.success(contextoActual()!!)
    } catch (e: CancellationException) { throw e
    } catch (e: Exception) { Result.failure(e) }

    override suspend fun recuperarSesion(): Result<Sesion?> {
        val token = almacenSeguro.obtenerToken()
        if (token.isNullOrBlank()) return Result.success(null)
        return try {
            val dto: UsuarioActualDto = client.get("api/v1/auth/me") { bearerAuth(token) }.body()
            val sesion = dto.toDomain(token)
            val anterior = almacenSeguro.obtenerPerfil()?.takeIf { it.username == dto.username }
            // /me does not provide a fresh expiry; an unknown deadline cannot authorize writes.
            val ahora = reloj.ahoraMs()
            val perfil = PerfilLocal(sesion.username, sesion.nombreCompleto, sesion.roles, sesion.permisos,
                anterior?.validadoEnMs ?: ahora, anterior?.expiraTokenEnMs ?: ahora)
            almacenSeguro.guardarPerfil(perfil)
            activar(sesion, perfil, ModoAcceso.REMOTO)
            Result.success(sesion)
        } catch (e: CancellationException) { throw e
        } catch (e: Throwable) {
            if (e is ClientRequestException && e.response.status.value == 401) {
                almacenSeguro.eliminarToken()
                client.limpiarTokensAuth()
                notificar401()
                Result.failure(SesionException(ErrorSesion.SesionExpirada()))
            } else Result.failure(SesionException(e.toErrorSesion()))
        }
    }
    override suspend fun cerrarSesion() {
        ++generacion
        _sesionActual.value = null
        _acceso.value = AccesoSesion.SinSesion
        perfilActual = null
        almacenSeguro.eliminarToken()
        almacenSeguro.borrarContinuidad()
        client.limpiarTokensAuth()
    }
    override fun notificar401() {
        val contexto = contextoActual()
        val perfil = perfilActual
        _sesionActual.value = null
        _acceso.value = if(contexto != null && perfil != null)
            AccesoSesion.Reautenticar(contexto, perfil, "Sesión expirada") else AccesoSesion.SinSesion
        _evento401.tryEmit(Unit)
    }
    override fun notificar403(mensaje: String) { _evento403.tryEmit(mensaje) }
}
