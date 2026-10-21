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
    fun test1_descargaConRedGuardaProductosRemotos() = runTest(testDispatcher) {
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

        // Sincronizar catálogo
        repo.sincronizar()

        val productos = repo.observar().first()
        assertEquals(2, productos.size)
        assertEquals("Amoxicilina 500mg", productos[0].nombre)
        assertEquals(101L, productos[0].id)
        assertEquals(0, local.pendientes().size, "No debe haber productos pendientes tras sincronizar")
    }

    @Test
    fun test2_registroSinRedGuardaComoPendienteConIdNegativo() = runTest(testDispatcher) {
        // Simular que no hay conexión (servidor devuelve 503 o falla)
        val (client, _) = crearMockClient(
            onPost = { HttpStatusCode.ServiceUnavailable to "{\"message\":\"Sin conexion\"}" },
            onGet = { HttpStatusCode.ServiceUnavailable to "{\"message\":\"Sin conexion\"}" }
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

        // Verificaciones
        assertTrue(guardado.id < 0L, "El ID local debe ser negativo (ej: -1)")
        assertEquals(-1L, guardado.id)

        val lista = repo.observar().first()
        assertEquals(1, lista.size)
        assertEquals("Vitamina C 1000mg", lista.first().nombre)

        val pendientes = local.pendientes()
        assertEquals(1, pendientes.size)
        assertEquals(-1L, pendientes.first().id)
    }

    @Test
    fun test3_reconexionExitosaReconciliaTemporalYRemoto() = runTest(testDispatcher) {
        // Guardar previamente un pendiente en base local
        val localP = local.guardarPendiente(
            Producto(id = 0L, nombre = "Paracetamol Jarabe", precio = 9.5, stock = 15)
        )
        assertEquals(-1L, localP.id)
        assertEquals(1, local.pendientes().size)

        // Servidor confirma el registro y asigna ID remoto 45
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

        // Ejecutar sincronización (reconectar)
        repo.sincronizar()

        // Verificaciones post-reconciliación
        assertEquals(0, local.pendientes().size, "La lista de pendientes debe quedar vacía")
        val lista = repo.observar().first()
        assertEquals(1, lista.size)
        assertEquals(45L, lista.first().id, "El producto debe conservar el ID remoto confirmado (45)")
    }

    @Test
    fun test4_segundaSincronizacionNoVuelveAEnviarRegistrosConfirmados() = runTest(testDispatcher) {
        local.guardarPendiente(
            Producto(id = 0L, nombre = "Alcohol 70%", precio = 6.0, stock = 25)
        )

        val respuestaPost = """
        {"id": 88, "nombre": "Alcohol 70%", "precio": 6.0, "stock": 25, "estado": true, "categoriaId": 26}
        """.trimIndent()

        val respuestaGet = """
        {
          "contenido": [
            {"id": 88, "nombre": "Alcohol 70%", "precio": 6.0, "stock": 25, "estado": true, "categoriaId": 26}
          ],
          "pagina": 0, "tamanio": 20, "totalElementos": 1, "totalPaginas": 1, "ultima": true
        }
        """.trimIndent()

        val (client, getPostCount) = crearMockClient(
            onPost = { HttpStatusCode.Created to respuestaPost },
            onGet = { HttpStatusCode.OK to respuestaGet }
        )

        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        // 1ra Sincronización -> Debe enviar 1 POST
        repo.sincronizar()
        assertEquals(1, getPostCount(), "Se debió emitir exactamente 1 petición POST")

        // 2da Sincronización -> 0 POST adicionales
        repo.sincronizar()
        assertEquals(1, getPostCount(), "La segunda sincronización no debe volver a emitir POST")
    }

    @Test
    fun test5_postExitosoYGetFallidoNoPierdeLaConfirmacionLocal() = runTest(testDispatcher) {
        val p = local.guardarPendiente(
            Producto(id = 0L, nombre = "Gasas Estériles", precio = 3.5, stock = 100)
        )
        assertEquals(-1L, p.id)

        var postLlamado = false
        val mockEngine = MockEngine { request ->
            if (request.method.value == "POST") {
                postLlamado = true
                respond(
                    content = """{"id": 99, "nombre": "Gasas Estériles", "precio": 3.5, "stock": 100, "estado": true, "categoriaId": 26}""",
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            } else {
                // El GET subsiguiente falla intencionalmente (ej: micro-corte de red)
                respond(
                    content = "Error de red",
                    status = HttpStatusCode.BadGateway
                )
            }
        }

        val client = HttpClient(mockEngine) {
            expectSuccess = true
            install(ContentNegotiation) { json(json) }
            defaultRequest {
                url("http://localhost:8080/")
                contentType(ContentType.Application.Json)
            }
        }

        val api = ProductoApi(client)
        val repo = ProductoRepositoryOffline(local, api)

        // Ejecutar sincronización que fallará en la fase 2 (GET)
        val resultado = runCatching { repo.sincronizar() }
        assertTrue(resultado.isFailure, "La sincronización reporta fallo porque el GET no completó")
        assertTrue(postLlamado, "El POST sí fue ejecutado exitosamente")

        // Verificación crítica: La reconciliación del POST no se perdió
        assertEquals(0, local.pendientes().size, "El producto ya no debe estar marcado como pendiente")
        val lista = repo.observar().first()
        assertEquals(1, lista.size)
        assertEquals(99L, lista.first().id, "El ID 99 remoto confirmado se conservó en disco")
    }

    @Test
    fun test6_reemplazoDeListadoRemotoNoEliminaPendientesLocales() = runTest(testDispatcher) {
        // Guardar un producto pendiente localmente (-1)
        val pendiente = local.guardarPendiente(
            Producto(id = 0L, nombre = "Pendiente Offline", precio = 20.0, stock = 5)
        )
        assertEquals(-1L, pendiente.id)

        // Nuevos datos remotos descargados del servidor
        val remotos = listOf(
            Producto(id = 10L, nombre = "Remoto 1", precio = 10.0, stock = 10),
            Producto(id = 20L, nombre = "Remoto 2", precio = 15.0, stock = 15)
        )

        // Ejecutar reemplazo de sincronizados
        local.reemplazarSincronizados(remotos)

        // Verificaciones
        val productos = local.observar().first()
        assertEquals(3, productos.size, "Debe contener los 2 remotos más el pendiente local")
        assertTrue(productos.any { it.id == -1L && it.nombre == "Pendiente Offline" }, "El pendiente local debe persistir")
        assertTrue(productos.any { it.id == 10L }, "Debe contener Remoto 1")
        assertTrue(productos.any { it.id == 20L }, "Debe contener Remoto 2")
        assertEquals(1, local.pendientes().size, "Sigue existiendo exactamente 1 pendiente")
    }
}
