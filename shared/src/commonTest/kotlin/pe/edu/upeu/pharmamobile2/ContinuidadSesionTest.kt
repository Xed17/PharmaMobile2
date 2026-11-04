package pe.edu.upeu.pharmamobile2

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobile2.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.testutil.RelojFalso
import kotlin.test.*

class ContinuidadSesionTest {
    private fun cliente() = HttpClient(MockEngine {
        respond("""{"token":"token-A","username":"Ana","nombreCompleto":"Ana Prueba","expiraEnSegundos":60,"roles":["VENDEDOR"],"permisos":["CATALOGO_LEER"]}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json() }; expectSuccess = true; defaultRequest { contentType(ContentType.Application.Json) } }

    @Test fun primeraAperturaNoInventaAdministrador() = runTest {
        cliente().use { client ->
            val repo = AuthRepositorioRest(client, FakeAlmacenSeguro(), RelojFalso(1000))
            assertTrue(repo.habilitarContinuidadLocal().isFailure)
            assertIs<AccesoSesion.SinSesion>(repo.acceso.value)
        }
    }
    @Test fun perfilValidadoRestauraLocalSinHabilitarTokenRemoto() = runTest {
        cliente().use { client ->
            val almacen = FakeAlmacenSeguro()
            val reloj = RelojFalso(1000)
            val primero = AuthRepositorioRest(client, almacen, reloj)
            assertTrue(primero.iniciarSesion(Credenciales("Ana", "prueba")).isSuccess)
            val restaurado = AuthRepositorioRest(client, almacen, reloj)
            val contexto = restaurado.habilitarContinuidadLocal().getOrThrow()
            val acceso = assertIs<AccesoSesion.Disponible>(restaurado.acceso.value)
            assertEquals(ModoAcceso.LOCAL, acceso.modo)
            assertEquals(setOf("CATALOGO_LEER"), acceso.perfil.permisos)
            assertNull(restaurado.sesionRemota(contexto))
            assertTrue(restaurado.esActual(contexto))
        }
    }
    @Test fun cerrarSesionBloqueaReaperturaOffline() = runTest {
        cliente().use { client ->
            val almacen = FakeAlmacenSeguro()
            val repo = AuthRepositorioRest(client, almacen, RelojFalso(1000))
            repo.iniciarSesion(Credenciales("Ana", "prueba")).getOrThrow()
            repo.cerrarSesion()
            assertNull(almacen.obtenerToken())
            assertTrue(AuthRepositorioRest(client, almacen, RelojFalso(1000)).habilitarContinuidadLocal().isFailure)
        }
    }
    @Test fun vencimientoYRetrocesoDeRelojNoExtiendenSesionRemota() = runTest {
        cliente().use { client ->
            val reloj = RelojFalso(1000)
            val repo = AuthRepositorioRest(client, FakeAlmacenSeguro(), reloj)
            repo.iniciarSesion(Credenciales("Ana", "prueba")).getOrThrow()
            val ctx = assertIs<AccesoSesion.Disponible>(repo.acceso.value).contexto
            assertNotNull(repo.sesionRemota(ctx))
            reloj.instanteMs = 999
            assertNull(repo.sesionRemota(ctx))
            reloj.instanteMs = 61000
            assertNull(repo.sesionRemota(ctx))
        }
    }
}
