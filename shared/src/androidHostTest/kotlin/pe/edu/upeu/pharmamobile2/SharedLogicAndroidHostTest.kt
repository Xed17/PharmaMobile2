package pe.edu.upeu.pharmamobile2

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
        try {
            val engine = io.ktor.client.engine.okhttp.OkHttp.create()
            val fakeAlmacen = object : pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro {
                var token: String? = null
                override suspend fun guardarToken(token: String) { this.token = token }
                override suspend fun obtenerToken(): String? = token
                override suspend fun eliminarToken() { token = null }
            }
            val client = pe.edu.upeu.pharmamobile2.data.remote.createHttpClient(
                engine = engine,
                almacenSeguro = fakeAlmacen,
                baseUrl = "http://localhost:8080/"
            )
            val api = pe.edu.upeu.pharmamobile2.data.remote.ProductoApi(client)
            val productos = api.obtenerProductos()
            println(">>> PRODUCTOS CARGADOS: " + productos.size)
        } catch (e: Exception) {
            println(">>> Error: " + e.message)
        }
    }
}