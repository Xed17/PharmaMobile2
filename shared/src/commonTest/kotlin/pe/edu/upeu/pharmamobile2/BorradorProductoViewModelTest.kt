package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.BorradorProductoLocal
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.SincronizadorProductos
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.*
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobile2.testutil.crearDriverPruebas
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class BorradorProductoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearFakeRepo(): ProductoRepository = object : ProductoRepository {
        override fun observar() = flowOf(emptyList<Producto>())
        override suspend fun listar(): List<Producto> = emptyList()
        override suspend fun obtener(id: Long): Producto = Producto(id, "Test", 10.0, 5)
        override suspend fun registrar(producto: Producto): Producto = producto
        override suspend fun actualizar(producto: Producto): Producto = producto
        override suspend fun eliminar(id: Long) {}
    }

    private fun crearFakeSincronizador(): SincronizadorProductos = object : SincronizadorProductos {
        override val actividad = MutableStateFlow(false)
        override val resultados = MutableSharedFlow<ResumenSincronizacion>()
        override fun iniciar() {}
        override suspend fun sincronizar(motivo: MotivoSync) = ResumenSincronizacion(0, 0, 0, 0, 0, false, null)
        override suspend fun reintentarDescarga() = ResumenSincronizacion(0, 0, 0, 0, 0, false, null)
        override suspend fun detener() {}
    }

    @Test
    fun formularioRestauraSoloSuCuenta() = runTest(testDispatcher) {
        val driver = crearDriverPruebas()
        try {
            val db = PharmaDb(driver)
            val cuentaA = ContextoCuenta("cuenta-A", 1)
            val cuentaB = ContextoCuenta("cuenta-B", 2)
            val borradorRepoA = BorradorProductoLocal(db, cuentaA, testDispatcher)
            val borradorRepoB = BorradorProductoLocal(db, cuentaB, testDispatcher)

            borradorRepoA.guardar(BorradorProducto(null, "Aspirina", "12.", "5", 26L, "Analgésicos"))
            advanceUntilIdle()

            val fakeRepo = crearFakeRepo()
            val fakeSync = crearFakeSincronizador()

            val vmB = ProductoViewModel(
                observarProductos = ObservarProductosUseCase(fakeRepo),
                sincronizarProductos = SincronizarProductosUseCase(fakeSync),
                listarProductos = ListarProductosUseCase(fakeRepo),
                obtenerProducto = ObtenerProductoUseCase(fakeRepo),
                registrarProducto = RegistrarProductoUseCase(fakeRepo),
                actualizarProducto = ActualizarProductoUseCase(fakeRepo),
                eliminarProducto = EliminarProductoUseCase(fakeRepo),
                borradorRepository = borradorRepoB
            )
            advanceUntilIdle()
            assertEquals("", vmB.uiState.value.formulario.precio)

            val vmA = ProductoViewModel(
                observarProductos = ObservarProductosUseCase(fakeRepo),
                sincronizarProductos = SincronizarProductosUseCase(fakeSync),
                listarProductos = ListarProductosUseCase(fakeRepo),
                obtenerProducto = ObtenerProductoUseCase(fakeRepo),
                registrarProducto = RegistrarProductoUseCase(fakeRepo),
                actualizarProducto = ActualizarProductoUseCase(fakeRepo),
                eliminarProducto = EliminarProductoUseCase(fakeRepo),
                borradorRepository = borradorRepoA
            )
            advanceUntilIdle()
            val formularioRestaurado = vmA.uiState.value.formulario
            assertEquals("12.", formularioRestaurado.precio)
        } finally {
            advanceUntilIdle()
            driver.close()
        }
    }

    @Test
    fun guardarNoDiceConfirmadoSiSoloEsLocal() = runTest(testDispatcher) {
        val driver = crearDriverPruebas()
        try {
            val db = PharmaDb(driver)
            val borradorRepo = BorradorProductoLocal(db, ContextoCuenta("A", 1), testDispatcher)
            val fakeRepo = object : ProductoRepository {
                override fun observar() = flowOf(emptyList<Producto>())
                override suspend fun listar(): List<Producto> = emptyList()
                override suspend fun obtener(id: Long): Producto = Producto(id, "Test", 10.0, 5)
                override suspend fun registrar(producto: Producto): Producto = producto.copy(id = -1L)
                override suspend fun actualizar(producto: Producto): Producto = producto
                override suspend fun eliminar(id: Long) {}
            }
            val fakeSync = crearFakeSincronizador()

            val productoVm = ProductoViewModel(
                observarProductos = ObservarProductosUseCase(fakeRepo),
                sincronizarProductos = SincronizarProductosUseCase(fakeSync),
                listarProductos = ListarProductosUseCase(fakeRepo),
                obtenerProducto = ObtenerProductoUseCase(fakeRepo),
                registrarProducto = RegistrarProductoUseCase(fakeRepo),
                actualizarProducto = ActualizarProductoUseCase(fakeRepo),
                eliminarProducto = EliminarProductoUseCase(fakeRepo),
                borradorRepository = borradorRepo
            )

            productoVm.actualizarNombre("Ibuprofeno")
            productoVm.actualizarPrecio("15.5")
            productoVm.actualizarStock("10")
            productoVm.guardar()

            advanceUntilIdle()
            assertEquals("Guardado en este equipo. Pendiente de envío", productoVm.uiState.value.mensajeExito)
        } finally {
            advanceUntilIdle()
            driver.close()
        }
    }
}
