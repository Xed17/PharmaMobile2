package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import pe.edu.upeu.pharmamobile2.data.local.DescargaLocal
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.SincronizadorProductos
import pe.edu.upeu.pharmamobile2.domain.usecase.RevisarEnvioUseCase
import pe.edu.upeu.pharmamobile2.presentation.sincronizacion.SincronizacionViewModel
import pe.edu.upeu.pharmamobile2.testutil.EscenarioSync
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class SincronizacionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun contadoresNoOcultanFallidas() = runTest(testDispatcher) {
        val f = EscenarioSync.crear(testDispatcher)
        try {
            // Guardar producto que pasa a permanente fallida
            val reg1 = f.cola.guardar(Producto(0, "Prod Fallido", 10.0, 1))
            f.cola.iniciar(reg1.producto.id, 1000L)
            f.cola.resolver(reg1.producto.id, ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.PERMANENTE, "400", "Invalido")), 1000L)

            // Guardar producto que pasa a revision
            val reg2 = f.cola.guardar(Producto(0, "Prod Incierto", 20.0, 2))
            f.cola.iniciar(reg2.producto.id, 1000L)
            f.cola.resolver(reg2.producto.id, ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.TRANSITORIO, "TIMEOUT", "Timeout", efectoIncierto = true)), 1000L)

            val sincronizadorFake = object : SincronizadorProductos {
                override val actividad = MutableStateFlow(false)
                override val resultados = MutableSharedFlow<ResumenSincronizacion>()
                override fun iniciar() {}
                override suspend fun sincronizar(motivo: MotivoSync) = ResumenSincronizacion(0, 0, 0, 0, 0, false, null)
                override suspend fun reintentarDescarga() = ResumenSincronizacion(0, 0, 0, 0, 0, false, null)
                override suspend fun detener() {}
            }
            val disparador = pe.edu.upeu.pharmamobile2.data.sync.DisparadorSincronizacion()
            val revisarUseCase = RevisarEnvioUseCase(f.cola, f.auth, f.contexto, f.reloj, disparador)
            val vm = SincronizacionViewModel(
                cola = f.cola,
                descarga = DescargaLocal(f.db, f.contexto, testDispatcher),
                coordinador = sincronizadorFake,
                revisarEnvio = revisarUseCase,
                reloj = f.reloj,
                iniciarTicker = false
            )

            advanceUntilIdle()
            val estado = vm.estado.value
            assertEquals(0, estado.pendientes)
            assertEquals(1, estado.fallidas)
            assertEquals(1, estado.revision)
        } finally {
            advanceUntilIdle()
            f.cerrar()
        }
    }

    @Test
    fun antiguedadCambiaSinNuevoTimestamp() = runTest(testDispatcher) {
        val f = EscenarioSync.crear(testDispatcher)
        try {
            val descargaLocal = DescargaLocal(f.db, f.contexto, testDispatcher)
            val ahoraInicial = 10_000_000L
            f.reloj.instanteMs = ahoraInicial
            descargaLocal.conciliarCompleta(emptyList(), ahoraInicial)
            advanceUntilIdle()

            val sincronizadorFake = object : SincronizadorProductos {
                override val actividad = MutableStateFlow(false)
                override val resultados = MutableSharedFlow<ResumenSincronizacion>()
                override fun iniciar() {}
                override suspend fun sincronizar(motivo: MotivoSync) = ResumenSincronizacion(0, 0, 0, 0, 0, false, null)
                override suspend fun reintentarDescarga() = ResumenSincronizacion(0, 0, 0, 0, 0, false, null)
                override suspend fun detener() {}
            }
            val disparador = pe.edu.upeu.pharmamobile2.data.sync.DisparadorSincronizacion()
            val revisarUseCase = RevisarEnvioUseCase(f.cola, f.auth, f.contexto, f.reloj, disparador)
            val vm = SincronizacionViewModel(
                cola = f.cola,
                descarga = descargaLocal,
                coordinador = sincronizadorFake,
                revisarEnvio = revisarUseCase,
                reloj = f.reloj,
                iniciarTicker = false
            )

            advanceUntilIdle()
            assertEquals("Actualizado hace un momento", vm.estado.value.antiguedad)

            // Avanzar 2 horas (2 * 3600 * 1000 = 7_200_000 ms)
            f.reloj.instanteMs = ahoraInicial + 7_200_000L
            vm.avanzarReloj(f.reloj.instanteMs)
            advanceUntilIdle()

            val trasAvanzarReloj = vm.estado.value
            assertEquals("Actualizado hace 2 h", trasAvanzarReloj.antiguedad)
        } finally {
            advanceUntilIdle()
            f.cerrar()
        }
    }
}
