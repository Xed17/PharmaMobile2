package pe.edu.upeu.pharmamobile2

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRepositoryOffline
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.testutil.crearDriverPruebas
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProductoRepositoryOfflineTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private lateinit var driver: app.cash.sqldelight.db.SqlDriver
    private lateinit var db: PharmaDb
    private lateinit var local: ProductoLocal
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        driver = crearDriverPruebas()
        db = PharmaDb(driver)
        local = ProductoLocal(db, testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
    }

    private fun crearMockClient(
        onPost: (body: String) -> Pair<HttpStatusCode, String>,
        onGet: () -> Pair<HttpStatusCode, String>
    ): Pair<HttpClient, () -> Int> {
        var postCount = 0
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val method = request.method.value

            val (status, content) = if (method == "POST") {
                postCount++
                val bodyStr = (request.body as? io.ktor.http.content.TextContent)?.text ?: ""
                onPost(bodyStr)
            } else {
                onGet()
            }

            respond(
                content = content,
                status = status,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(json)
            }
            defaultRequest {
                url("http://localhost:8080/")
                contentType(ContentType.Application.Json)
            }
        }

        return Pair(client) { postCount }
    }

    @Test
    fun test1_sincronizacionConRed_flowEmiteListaCompleta() = runTest(testDispatcher) {
        val respuestaListado = """
        {
          "contenido": [
            {"id": 101, "nombre": "Amoxicilina 500mg", "precio": 12.5, "stock": 50, "estado": true, "categoriaId": 26},
            {"id": 102, "nombre": "Ibuprofeno 400mg", "precio": 8.0, "stock": 40, "estado": true, "categoriaId": 26}
          ],
          "pagina": 0, "tamanio": 20, "totalElementos": 2, "totalPaginas": 1, "ultima": true
        }
        """.trimIndent()

        val (client, _) = crearMockClient(
            onPost = { HttpStatusCode.Created to "{}" },
            onGet = { HttpStatusCode.OK to respuestaListado }
        )

        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        // Sincronizar catálogo con red disponible
        repo.sincronizar()

        // 1. La base contiene los productos del servidor y el Flow emite la lista completa
        val productos = repo.observar().first()
        assertEquals(2, productos.size, "El Flow debe emitir los 2 productos descargados")
        assertEquals(101L, productos[0].id)
        assertEquals("Amoxicilina 500mg", productos[0].nombre)
        assertEquals(102L, productos[1].id)
        assertEquals("Ibuprofeno 400mg", productos[1].nombre)
        assertEquals(0, local.pendientes().size, "No debe haber productos marcados como pendientes")
    }

    @Test
    fun test2_registroSinRed_guardaPendienteYFlowEmite() = runTest(testDispatcher) {
        // Al fallar el envío (sin red / 503)
        val (client, _) = crearMockClient(
            onPost = { HttpStatusCode.ServiceUnavailable to "{\"message\":\"Sin red\"}" },
            onGet = { HttpStatusCode.ServiceUnavailable to "{\"message\":\"Sin red\"}" }
        )

        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        val nuevo = Producto(
            id = 0L,
            nombre = "Vitamina C 1000mg",
            precio = 18.0,
            stock = 30
        )

        // Registrar sin red
        val guardado = repo.registrar(nuevo)

        // Verificaciones:
        // 1. Queda guardado con la marca de pendiente (ID negativo < 0)
        assertTrue(guardado.id < 0L, "El producto debe recibir un ID local negativo indicando pendiente")
        assertEquals(-1L, guardado.id)

        val pendientes = local.pendientes()
        assertEquals(1, pendientes.size, "Debe existir exactamente 1 producto pendiente en base local")
        assertEquals(-1L, pendientes.first().id)

        // 2. El Flow lo emite igualmente para que la UI lo pinte de inmediato
        val listaFlow = repo.observar().first()
        assertEquals(1, listaFlow.size, "El Flow debe emitir el producto guardado sin red")
        assertEquals("Vitamina C 1000mg", listaFlow.first().nombre)
        assertEquals(-1L, listaFlow.first().id)
    }

    @Test
    fun test3_envioAlReconectar_eliminaMarcaPendiente() = runTest(testDispatcher) {
        // 1. Previamente guardado sin red con marca de pendiente (-1)
        val localP = local.guardarPendiente(
            Producto(id = 0L, nombre = "Paracetamol Jarabe", precio = 9.5, stock = 15)
        )
        assertEquals(-1L, localP.id)
        assertEquals(1, local.pendientes().size)

        // 2. Servidor ahora disponible: confirma el POST y asigna ID real 45
        val respuestaPost = """
        {"id": 45, "nombre": "Paracetamol Jarabe", "precio": 9.5, "stock": 15, "estado": true, "categoriaId": 26}
        """.trimIndent()

        val respuestaGet = """
        {
          "contenido": [
            {"id": 45, "nombre": "Paracetamol Jarabe", "precio": 9.5, "stock": 15, "estado": true, "categoriaId": 26}
          ],
          "pagina": 0, "tamanio": 20, "totalElementos": 1, "totalPaginas": 1, "ultima": true
        }
        """.trimIndent()

        val (client, _) = crearMockClient(
            onPost = { HttpStatusCode.Created to respuestaPost },
            onGet = { HttpStatusCode.OK to respuestaGet }
        )

        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        // Sincronizar con el servidor disponible
        repo.sincronizar()

        // Verificaciones:
        // La fila deja de estar marcada como pendiente
        assertEquals(0, local.pendientes().size, "La lista de pendientes debe quedar en 0 tras reconexión exitosa")

        val lista = repo.observar().first()
        assertEquals(1, lista.size)
        assertEquals(45L, lista.first().id, "El producto consolidado tiene el ID real del servidor (45)")
    }

    @Test
    fun test4_laBajadaNoBorraLoPendiente_conservaCreadosSinRed() = runTest(testDispatcher) {
        val (client, _) = crearMockClient(
            onPost = { HttpStatusCode.ServiceUnavailable to "{}" },
            onGet = { HttpStatusCode.OK to "{}" }
        )
        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        // 1. El usuario creó un producto sin conexión
        val pendiente = local.guardarPendiente(
            Producto(id = 0L, nombre = "Antigripal Noche", precio = 14.0, stock = 20)
        )
        assertEquals(-1L, pendiente.id)

        // 2. Traer la lista del servidor
        val remotos = listOf(
            Producto(id = 10L, nombre = "Omeprazol 20mg", precio = 10.0, stock = 35),
            Producto(id = 20L, nombre = "Loratadina 10mg", precio = 6.5, stock = 50)
        )
        local.reemplazarSincronizados(remotos)

        // Verificación esencial: Traer la lista del servidor conserva las filas que el usuario creó sin conexión
        val listado = repo.observar().first()
        assertEquals(3, listado.size, "Deben convivir los 2 productos del servidor y el creado sin conexión")
        assertTrue(listado.any { it.id == -1L && it.nombre == "Antigripal Noche" }, "El producto pendiente no fue borrado")
        assertTrue(listado.any { it.id == 10L && it.nombre == "Omeprazol 20mg" })
        assertTrue(listado.any { it.id == 20L && it.nombre == "Loratadina 10mg" })
        assertEquals(1, local.pendientes().size, "Sigue registrado exactamente 1 pendiente")
    }

    @Test
    fun test5_identificadoresSinColision_convivenSinSobrescribirse() = runTest(testDispatcher) {
        val (client, _) = crearMockClient(
            onPost = { HttpStatusCode.Created to "{}" },
            onGet = { HttpStatusCode.OK to "{}" }
        )
        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        // Producto local pendiente con ID negativo (-1)
        local.guardarPendiente(
            Producto(id = 0L, nombre = "Producto Local Pendiente", precio = 25.0, stock = 8)
        )

        // Producto del servidor con ID numérico positivo (1)
        val productoServidor = listOf(
            Producto(id = 1L, nombre = "Producto Servidor 1", precio = 30.0, stock = 12)
        )
        local.reemplazarSincronizados(productoServidor)

        // Verificación: Un producto local y uno del servidor con numeración distinta conviven sin sobrescribirse
        val emitidos = repo.observar().first()
        assertEquals(2, emitidos.size, "Ambos productos deben convivir en la misma base local")
        
        val localEncontrado = emitidos.find { it.id == -1L }
        assertNotNull(localEncontrado, "El producto local con ID -1 debe existir")
        assertEquals("Producto Local Pendiente", localEncontrado.nombre)

        val remotoEncontrado = emitidos.find { it.id == 1L }
        assertNotNull(remotoEncontrado, "El producto remoto con ID 1 debe existir")
        assertEquals("Producto Servidor 1", remotoEncontrado.nombre)
    }

    @Test
    fun test6_sincronizarDosVecesNoDuplica_mismaCantidadFilas() = runTest(testDispatcher) {
        val respuestaListado = """
        {
          "contenido": [
            {"id": 201, "nombre": "Alcohol en Gel 500ml", "precio": 11.0, "stock": 60, "estado": true, "categoriaId": 26},
            {"id": 202, "nombre": "Mascarillas KN95 x10", "precio": 15.0, "stock": 40, "estado": true, "categoriaId": 26}
          ],
          "pagina": 0, "tamanio": 20, "totalElementos": 2, "totalPaginas": 1, "ultima": true
        }
        """.trimIndent()

        val (client, _) = crearMockClient(
            onPost = { HttpStatusCode.Created to "{}" },
            onGet = { HttpStatusCode.OK to respuestaListado }
        )

        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        // 1ra Sincronización
        repo.sincronizar()
        val conteo1 = repo.observar().first().size
        assertEquals(2, conteo1, "Primera sincronización carga 2 productos")

        // 2da Sincronización inmediata
        repo.sincronizar()
        val conteo2 = repo.observar().first().size
        assertEquals(2, conteo2, "Ejecutar la sincronización dos veces seguidas deja la misma cantidad de filas")
        assertEquals(conteo1, conteo2, "No debe haber duplicación de registros")
    }
}
