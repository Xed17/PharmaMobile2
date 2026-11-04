package pe.edu.upeu.pharmamobile2

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobile2.data.remote.*
import pe.edu.upeu.pharmamobile2.data.repository.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.testutil.RelojFalso
import kotlin.test.*

class ProductoRemotoSesion13Test {
    private fun cliente(motor: MockEngine)=HttpClient(motor) {
        install(ContentNegotiation) { json() }
        expectSuccess=true
        defaultRequest { contentType(ContentType.Application.Json) }
    }
    private fun login(user: String)="""{"token":"token-$user","username":"$user","nombreCompleto":"Prueba","expiraEnSegundos":3600}"""

    @Test fun capturaTokenYDescartaRespuestaDeGeneracionAnterior() = runTest {
        var user="A";var cabecera: String?=null
        val iniciada=CompletableDeferred<Unit>();val liberar=CompletableDeferred<Unit>()
        val client=cliente(MockEngine { request ->
            val body=if(request.url.encodedPath.contains("auth/login")) login(user) else {
                cabecera=request.headers[HttpHeaders.Authorization]
                iniciada.complete(Unit);liberar.await()
                """{"contenido":[],"pagina":0,"totalElementos":0,"totalPaginas":0,"ultima":true}"""
            }
            respond(body,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        })
        try {
            val auth=AuthRepositorioRest(client,FakeAlmacenSeguro(),RelojFalso(1000))
            auth.iniciarSesion(Credenciales("A","prueba")).getOrThrow()
            val ctx=(auth.acceso.value as AccesoSesion.Disponible).contexto
            val remoto=ProductoRemotoRest(ProductoApi(client),AutorizadorSolicitudes(auth),ctx)
            val pendiente=async { remoto.listarCompleto() }
            iniciada.await()
            user="B";auth.iniciarSesion(Credenciales("B","prueba")).getOrThrow()
            liberar.complete(Unit)
            assertFailsWith<CancellationException> { pendiente.await() }
            assertEquals("Bearer token-A",cabecera)
        } finally { client.close() }
    }
    @Test fun creacionCon503EsInciertaYNuncaSeDeclaraRepetible() = runTest {
        val client=cliente(MockEngine { request ->
            if(request.url.encodedPath.contains("auth/login")) respond(login("A"),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
            else respond("{}",HttpStatusCode.ServiceUnavailable,headersOf(HttpHeaders.ContentType,"application/json"))
        })
        try {
            val auth=AuthRepositorioRest(client,FakeAlmacenSeguro(),RelojFalso(1000))
            auth.iniciarSesion(Credenciales("A","prueba")).getOrThrow()
            val ctx=(auth.acceso.value as AccesoSesion.Disponible).contexto
            val remoto=ProductoRemotoRest(ProductoApi(client),AutorizadorSolicitudes(auth),ctx)
            val fallo=assertIs<ResultadoEnvio.Fallo>(remoto.crear(Producto(nombre="Prueba",precio=2.0,stock=3))).error
            assertEquals(NaturalezaFallo.TRANSITORIO,fallo.naturaleza)
            assertTrue(fallo.efectoIncierto)
        } finally { client.close() }
    }
    @Test fun jsonInvalidoNoSeDisfrazaComoFaltaDeRed() = runTest {
        val client=cliente(MockEngine { request ->
            if(request.url.encodedPath.contains("auth/login")) respond(login("A"),HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
            else respond("not-json",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        })
        try {
            val auth=AuthRepositorioRest(client,FakeAlmacenSeguro(),RelojFalso(1000))
            auth.iniciarSesion(Credenciales("A","prueba")).getOrThrow()
            val ctx=(auth.acceso.value as AccesoSesion.Disponible).contexto
            val remoto=ProductoRemotoRest(ProductoApi(client),AutorizadorSolicitudes(auth),ctx)
            val error=assertIs<FalloOperacionException>(remoto.listarCompleto().exceptionOrNull()).fallo
            assertEquals("DATOS_INVALIDOS",error.codigo)
            assertEquals(NaturalezaFallo.PERMANENTE,error.naturaleza)
        } finally { client.close() }
    }
}
