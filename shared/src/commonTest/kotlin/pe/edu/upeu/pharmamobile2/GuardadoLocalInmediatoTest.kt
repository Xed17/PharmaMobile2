package pe.edu.upeu.pharmamobile2

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRepositoryOffline
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.testutil.crearDriverPruebas
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GuardadoLocalInmediatoTest {
    @Test fun guardarNoEsperaALaRed() = runTest {
        val driver=crearDriverPruebas();val gate=CompletableDeferred<Unit>()
        var posts=0
        val client=HttpClient(MockEngine { posts++;gate.await();respond("{}",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) }) {
            install(ContentNegotiation) { json() };defaultRequest { contentType(ContentType.Application.Json) }
        }
        val local=ProductoLocal(PharmaDb(driver),ContextoCuenta("A",1),StandardTestDispatcher(testScheduler))
        val job=async { ProductoRepositoryOffline(local,ProductoApi(client)).registrar(Producto(nombre="Prueba",precio=2.0,stock=3)) }
        try {
            runCurrent()
            assertTrue(job.isCompleted)
            assertTrue(job.await().id<0)
            assertEquals(1,local.listar().size)
            assertEquals(0,posts)
        } finally { gate.complete(Unit);job.cancelAndJoin();client.close();driver.close() }
    }
    @Test fun precioInfinitoSeRechazaAntesDePersistir() = runTest {
        val driver=crearDriverPruebas()
        val client=HttpClient(MockEngine { error("No debe contactar red") })
        try {
            val local=ProductoLocal(PharmaDb(driver),ContextoCuenta("A",1),StandardTestDispatcher(testScheduler))
            val registrar=RegistrarProductoUseCase(ProductoRepositoryOffline(local,ProductoApi(client)))
            assertTrue(registrar(Producto(nombre="Prueba",precio=Double.POSITIVE_INFINITY,stock=3)).isFailure)
            assertTrue(local.listar().isEmpty())
        } finally { client.close();driver.close() }
    }
    @Test fun cancelacionNoSeConvierteEnResultadoFallidoNormal() = runTest {
        val driver=crearDriverPruebas();val client=HttpClient(MockEngine { error("No debe contactar red") })
        try {
            val base=ProductoRepositoryOffline(ProductoLocal(PharmaDb(driver),ContextoCuenta("A",1)),ProductoApi(client))
            val cancelante=object : pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository by base {
                override suspend fun registrar(producto: Producto): Producto { throw CancellationException("prueba") }
            }
            assertFailsWith<CancellationException> { RegistrarProductoUseCase(cancelante)(Producto(nombre="Prueba",precio=2.0,stock=1)) }
        } finally { client.close();driver.close() }
    }
    @Test fun perfilSinPermisoNoPuedeCrearPendientes() = runTest {
        val driver=crearDriverPruebas();var peticionesProductos=0
        val client=HttpClient(MockEngine { request ->
            if(request.url.encodedPath.contains("productos")) peticionesProductos++
            respond("""{"token":"token-prueba","username":"lector","nombreCompleto":"Lector","permisos":["CATALOGO_LEER"]}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() };expectSuccess=true;defaultRequest { contentType(ContentType.Application.Json) } }
        try {
            val auth=pe.edu.upeu.pharmamobile2.data.repository.AuthRepositorioRest(client,FakeAlmacenSeguro())
            auth.iniciarSesion(Credenciales("lector","prueba")).getOrThrow()
            val ctx=(auth.acceso.value as AccesoSesion.Disponible).contexto
            val local=ProductoLocal(PharmaDb(driver),ctx)
            val repo=ProductoRepositoryOffline(local,ProductoApi(client),authRepository=auth)
            val resultado=RegistrarProductoUseCase(repo)(Producto(nombre="Prueba",precio=2.0,stock=1))
            val error=assertIs<pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException>(resultado.exceptionOrNull()).error
            assertIs<pe.edu.upeu.pharmamobile2.domain.error.ErrorApi.SinPermisos>(error)
            assertTrue(local.listar().isEmpty())
            assertEquals(0,peticionesProductos)
        } finally { client.close();driver.close() }
    }

}
