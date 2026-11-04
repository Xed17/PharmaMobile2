package pe.edu.upeu.pharmamobile2

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import kotlin.test.*

class PaginacionCatalogoTest {
    @Test fun recorreTodasLasPaginasSinPerderProductos() = runTest {
        val paginas=mutableListOf<Int>()
        val client=HttpClient(MockEngine { request ->
            val pagina=request.url.parameters["pagina"]!!.toInt();paginas.add(pagina)
            val inicio=pagina*20+1;val fin=minOf(inicio+19,45)
            val contenido=(inicio..fin).joinToString(",") { id ->
                """{"id":$id,"nombre":"P$id","precio":3.5,"stock":10,"estado":${id!=21},"categoriaId":7}"""
            }
            respond("""{"contenido":[$contenido],"pagina":$pagina,"tamanio":20,"totalElementos":45,"totalPaginas":3,"ultima":${pagina==2}}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        }) { install(ContentNegotiation) { json() } }
        try {
            val productos=ProductoApi(client).obtenerProductos()
            assertEquals(45,productos.size)
            assertEquals(listOf(0,1,2),paginas)
            assertFalse(productos.single { it.id==21L }.estado)
            assertEquals(7L,productos.single { it.id==21L }.categoriaId)
        } finally { client.close() }
    }
    @Test fun paginaVaciaSinSerUltimaNoAceptaDescargaIncompleta() = runTest {
        val client=HttpClient(MockEngine { respond("""{"contenido":[],"pagina":0,"tamanio":20,"totalElementos":45,"totalPaginas":3,"ultima":false}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) }) { install(ContentNegotiation) { json() } }
        try { assertFails { ProductoApi(client).obtenerProductos() } } finally { client.close() }
    }
}
