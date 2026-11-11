package pe.edu.upeu.pharmamobile2

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import pe.edu.upeu.pharmamobile2.data.InMemoryRepository
import pe.edu.upeu.pharmamobile2.data.local.*
import pe.edu.upeu.pharmamobile2.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobile2.testutil.*
import kotlin.test.*
import pe.edu.upeu.pharmamobile2.data.LimpiadorDatosDemo

class AislamientoCuentaTest {

    @Test
    fun respuestaDeLoginPosteriorASalidaNoReactivaLaCuenta() = runTest {
        val iniciada = CompletableDeferred<Unit>()
        val liberar = CompletableDeferred<Unit>()
        val client = HttpClient(MockEngine {
            iniciada.complete(Unit)
            liberar.await()
            respond("""{"token":"A","username":"A","nombreCompleto":"A"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }) {
            install(ContentNegotiation) { json() }
            defaultRequest { contentType(ContentType.Application.Json) }
        }
        val almacen = FakeAlmacenSeguro()
        val auth = AuthRepositorioRest(client, almacen, RelojFalso(1000))
        try {
            val login = async { auth.iniciarSesion(Credenciales("A", "prueba")) }
            iniciada.await()
            auth.cerrarSesion()
            liberar.complete(Unit)
            try { login.await() } catch (_: CancellationException) { }
            assertEquals(AccesoSesion.SinSesion, auth.acceso.value)
            assertNull(almacen.obtenerToken())
            assertNull(almacen.obtenerPerfil())
        } finally {
            client.close()
        }
    }

    @Test
    fun respuestaTardiaDeANoConfirmaNiEscribeEnB() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val f = EscenarioSync.crear(dispatcher)
        val iniciada = CompletableDeferred<Unit>()
        val liberar = CompletableDeferred<Unit>()
        try {
            val id = f.cola.guardar(Producto(0, "Privado A", 1.0, 1)).producto.id
            f.remoto.respuesta = { p ->
                withContext(NonCancellable) {
                    iniciada.complete(Unit)
                    liberar.await()
                    ResultadoEnvio.Exito(p.copy(id = 99))
                }
            }
            val envio = async { f.enviar() }
            iniciada.await()
            f.auth.cerrarSesion()
            envio.cancel()
            liberar.complete(Unit)
            try { envio.await() } catch (_: CancellationException) { }
            assertEquals(EstadoEnvio.ENVIANDO, f.cola.porId(id)!!.estado)
            val b = ProductoLocal(f.db, ContextoCuenta("B", 2), dispatcher)
            assertTrue(b.listar().isEmpty())
            val borradorA = BorradorProductoLocal(f.db, f.contexto, dispatcher)
            borradorA.guardar(BorradorProducto(null, "Privado A", "", "", null, null))
            assertNull(BorradorProductoLocal(f.db, ContextoCuenta("B", 2), dispatcher).observar().first())
            f.cola.recuperarInterrumpidos()
            assertEquals(EstadoEnvio.REVISION, f.cola.porId(id)!!.estado)
            assertEquals(1, f.remoto.llamadas)
        } finally {
            liberar.complete(Unit)
            f.cerrar()
        }
    }

    @Test
    fun salidaConPostEnCursoNoReenviaAlVolver() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val f = EscenarioSync.crear(dispatcher)
        try {
            val id = f.cola.guardar(Producto(0, "Pendiente A", 10.0, 5)).producto.id
            f.cola.iniciar(id, 1000L)
            // Logout happens while in flight
            f.auth.cerrarSesion()
            f.cola.recuperarInterrumpidos()
            val registro = f.cola.porId(id)!!
            assertEquals(EstadoEnvio.REVISION, registro.estado)
            // Elegibles does not include REVISION
            assertTrue(f.cola.elegibles().none { it.producto.id == id })
        } finally {
            f.cerrar()
        }
    }

    @Test
    fun borradorAInvisibleEnB() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val driver = crearDriverPruebas()
        try {
            val db = pe.edu.upeu.pharmamobil.db.PharmaDb(driver)
            val cuentaA = ContextoCuenta("cuenta-A", 1)
            val cuentaB = ContextoCuenta("cuenta-B", 2)
            val repoA = BorradorProductoLocal(db, cuentaA, testDispatcher)
            val repoB = BorradorProductoLocal(db, cuentaB, testDispatcher)

            repoA.guardar(BorradorProducto(null, "Borrador Secreto A", "12.50", "10", 1L, "Gastro"))
            assertEquals("Borrador Secreto A", repoA.observar().first()?.nombre)
            assertNull(repoB.observar().first())
        } finally {
            driver.close()
        }
    }

    @Test
    fun demoNoConservaClienteDeOtroUsuario() = runTest {
        val fakeAuth = object : pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository {
            override val acceso = kotlinx.coroutines.flow.MutableStateFlow<AccesoSesion>(AccesoSesion.SinSesion)
            override val sesionActual = kotlinx.coroutines.flow.MutableStateFlow<Sesion?>(null)
            override val evento401 = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
            override val evento403 = kotlinx.coroutines.flow.MutableSharedFlow<String>()
            override suspend fun habilitarContinuidadLocal() = Result.failure<ContextoCuenta>(Exception())
            override suspend fun sesionRemota(contexto: ContextoCuenta): Sesion? = null
            override fun esActual(contexto: ContextoCuenta) = false
            override suspend fun iniciarSesion(credenciales: Credenciales) = Result.failure<Sesion>(Exception())
            override suspend fun recuperarSesion() = Result.success(null)
            override suspend fun cerrarSesion() {}
            override fun notificar401() {}
            override fun notificar403(mensaje: String) {}
        }
        val cerrarSesionUseCase = CerrarSesionUseCase(fakeAuth, LimpiadorDatosDemo())

        InMemoryRepository.agregarCliente(Cliente(999L, "Cliente privado A", "a@test.com", "999888777"))
        assertTrue(InMemoryRepository.clientes.any { it.nombre == "Cliente privado A" })

        cerrarSesionUseCase()
        val clientesDemoTrasCambio = InMemoryRepository.clientes
        assertFalse(clientesDemoTrasCambio.any { it.nombre == "Cliente privado A" })
    }
}
