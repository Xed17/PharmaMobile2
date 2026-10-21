package pe.edu.upeu.pharmamobile2

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile2.data.remote.createHttpClient
import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicAndroidHostTest {

    @Test
    fun clienteTelefono() {
        val cliente = Cliente(
            id = 1L,
            nombre = "Farmacia Central",
            correo = "ventas@central.pe",
            telefono = "987654321"
        )
        val resultado = cliente.obtenerTelefono()
        assertEquals("987654321", resultado)
    }

    @Test
    fun testKtorPeticionGetProductosYCodigo200() = kotlinx.coroutines.runBlocking {
        val mockJson = """
            {
              "contenido": [
                {
                  "id": 1,
                  "nombre": "Paracetamol 500mg",
                  "precio": 3.50,
                  "stock": 50,
                  "estado": true,
                  "categoriaId": 26,
                  "categoriaNombre": "Analgésicos"
                }
              ],
              "pagina": 0,
              "tamanio": 20,
              "totalElementos": 1,
              "totalPaginas": 1,
              "ultima": true
            }
        """.trimIndent()

        val mockEngine = MockEngine {
            respond(
                content = mockJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val fakeAlmacen = object : AlmacenSeguro {
            override suspend fun guardarToken(token: String) {}
            override suspend fun obtenerToken(): String? = null
            override suspend fun eliminarToken() {}
        }

        val client = createHttpClient(
            engine = mockEngine,
            almacenSeguro = fakeAlmacen,
            baseUrl = "http://localhost:8080/"
        )

        val api = pe.edu.upeu.pharmamobile2.data.remote.ProductoApi(client)
        val productos = api.obtenerProductos()

        assertEquals(1, productos.size)
        assertEquals("Paracetamol 500mg", productos.first().nombre)
        assertEquals(3.50, productos.first().precio)
    }
}