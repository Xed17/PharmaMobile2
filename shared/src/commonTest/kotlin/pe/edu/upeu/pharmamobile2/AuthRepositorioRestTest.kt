package pe.edu.upeu.pharmamobile2

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile2.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobile2.domain.model.Credenciales
import pe.edu.upeu.pharmamobile2.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobile2.domain.model.SesionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeAlmacenSeguro(
    var tokenGuardado: String? = null
) : AlmacenSeguro {
    override suspend fun guardarToken(token: String) {
        tokenGuardado = token
    }

    override suspend fun obtenerToken(): String? = tokenGuardado

    override suspend fun eliminarToken() {
        tokenGuardado = null
    }
}

class AuthRepositorioRestTest {

    private fun crearMockClient(
        responder: (url: String) -> Pair<HttpStatusCode, String>
    ): HttpClient {
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val (status, content) = responder(path)
            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        return HttpClient(mockEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = true
                    }
                )
            }
            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
    }

    @Test
    fun testIniciarSesion_exitoso_guardaTokenYActualizaSesion() = runTest {
        val jsonLoginResponse = """
            {
                "token": "jwt.admin.valid.token",
                "tipo": "Bearer",
                "expiraEnSegundos": 3600,
                "username": "admin",
                "nombreCompleto": "Administrador General",
                "roles": ["ROLE_ADMIN"],
                "permisos": ["CATALOGO_ESCRIBIR", "CATALOGO_LEER"]
            }
        """.trimIndent()

        val client = crearMockClient { HttpStatusCode.OK to jsonLoginResponse }
        val fakeAlmacen = FakeAlmacenSeguro()
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        val resultado = repo.iniciarSesion(Credenciales("admin", "Admin123"))

        assertTrue(resultado.isSuccess)
        val sesion = resultado.getOrNull()
        assertNotNull(sesion)
        assertEquals("jwt.admin.valid.token", sesion.token)
        assertEquals("admin", sesion.username)
        assertEquals("jwt.admin.valid.token", fakeAlmacen.tokenGuardado)
        assertEquals(sesion, repo.sesionActual.value)
        assertTrue(sesion.puede("CATALOGO_ESCRIBIR"))
    }

    @Test
    fun testIniciarSesion_credencialesVacias_fallaSinRed() = runTest {
        val client = crearMockClient { HttpStatusCode.InternalServerError to "{}" }
        val fakeAlmacen = FakeAlmacenSeguro()
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        val resultado = repo.iniciarSesion(Credenciales("", ""))

        assertTrue(resultado.isFailure)
        assertNull(fakeAlmacen.tokenGuardado)
        val excepcion = resultado.exceptionOrNull() as? SesionException
        assertTrue(excepcion?.error is ErrorSesion.CredencialesIncorrectas)
    }

    @Test
    fun testIniciarSesion_credencialesIncorrectas_retorna401() = runTest {
        val jsonError = """
            {
                "status": 401,
                "message": "Usuario o contraseña incorrectos",
                "path": "/api/v1/auth/login"
            }
        """.trimIndent()

        val client = crearMockClient { HttpStatusCode.Unauthorized to jsonError }
        val fakeAlmacen = FakeAlmacenSeguro()
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        val resultado = repo.iniciarSesion(Credenciales("admin", "ClaveErronea"))

        assertTrue(resultado.isFailure)
        assertNull(fakeAlmacen.tokenGuardado)
        val excepcion = resultado.exceptionOrNull() as? SesionException
        assertTrue(excepcion?.error is ErrorSesion.CredencialesIncorrectas)
    }

    @Test
    fun testRecuperarSesion_sinToken_retornaNull() = runTest {
        val client = crearMockClient { HttpStatusCode.OK to "{}" }
        val fakeAlmacen = FakeAlmacenSeguro(tokenGuardado = null)
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        val resultado = repo.recuperarSesion()

        assertTrue(resultado.isSuccess)
        assertNull(resultado.getOrNull())
        assertNull(repo.sesionActual.value)
    }

    @Test
    fun testRecuperarSesion_conTokenValido_reconstruyeSesion() = runTest {
        val jsonUsuarioActual = """
            {
                "id": 1,
                "username": "vendedor",
                "empleadoId": 2,
                "empleadoNombre": "Maria Vendedora",
                "cargo": "Vendedor",
                "roles": ["ROLE_VENDEDOR"],
                "permisos": ["CATALOGO_LEER", "VENTAS_REGISTRAR"],
                "estado": true
            }
        """.trimIndent()

        val client = crearMockClient { HttpStatusCode.OK to jsonUsuarioActual }
        val fakeAlmacen = FakeAlmacenSeguro(tokenGuardado = "jwt.vendedor.existente")
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        val resultado = repo.recuperarSesion()

        assertTrue(resultado.isSuccess)
        val sesion = resultado.getOrNull()
        assertNotNull(sesion)
        assertEquals("jwt.vendedor.existente", sesion.token)
        assertEquals("vendedor", sesion.username)
        assertEquals("Maria Vendedora", sesion.nombreCompleto)
        assertEquals(sesion, repo.sesionActual.value)
    }

    @Test
    fun testRecuperarSesion_tokenExpirado401_eliminaTokenYNotifica() = runTest {
        val json401 = """{"status": 401, "message": "Debe iniciar sesión"}"""
        val client = crearMockClient { HttpStatusCode.Unauthorized to json401 }
        val fakeAlmacen = FakeAlmacenSeguro(tokenGuardado = "jwt.token.expirado")
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        val resultado = repo.recuperarSesion()

        assertTrue(resultado.isFailure)
        assertNull(fakeAlmacen.tokenGuardado) // Debe haber eliminado el token
        assertNull(repo.sesionActual.value)
        val excepcion = resultado.exceptionOrNull() as? SesionException
        assertTrue(excepcion?.error is ErrorSesion.SesionExpirada)
    }

    @Test
    fun testRecuperarSesion_falloDeRed_conservaToken() = runTest {
        // En caso de fallo de red, el token NO debe eliminarse
        val mockEngine = MockEngine {
            throw io.ktor.client.plugins.HttpRequestTimeoutException("http://10.0.2.2:8080/api/v1/auth/me", 5000L)
        }
        val client = HttpClient(mockEngine)
        val fakeAlmacen = FakeAlmacenSeguro(tokenGuardado = "jwt.token.offline")
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        val resultado = repo.recuperarSesion()

        assertTrue(resultado.isFailure)
        assertEquals("jwt.token.offline", fakeAlmacen.tokenGuardado) // Conserva token
        val excepcion = resultado.exceptionOrNull() as? SesionException
        assertTrue(excepcion?.error is ErrorSesion.SinConexion)
    }

    @Test
    fun testCerrarSesion_eliminaTokenYVaciaEstado() = runTest {
        val client = crearMockClient { HttpStatusCode.OK to "{}" }
        val fakeAlmacen = FakeAlmacenSeguro(tokenGuardado = "token.activo")
        val repo = AuthRepositorioRest(client, fakeAlmacen)

        repo.cerrarSesion()

        assertNull(fakeAlmacen.tokenGuardado)
        assertNull(repo.sesionActual.value)
    }
}
