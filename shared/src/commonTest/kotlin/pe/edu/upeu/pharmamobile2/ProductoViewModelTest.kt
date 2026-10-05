package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApi
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoUiState
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeProductoRepository(
        var productos: MutableList<Producto> = mutableListOf(),
        var throwOnListar: Throwable? = null,
        var throwOnRegistrar: Throwable? = null,
        var throwOnActualizar: Throwable? = null,
        var throwOnEliminar: Throwable? = null
    ) : ProductoRepository {
        var eliminarLlamadoId: Long? = null
        var registrarLlamado: Boolean = false

        override suspend fun listar(): List<Producto> {
            throwOnListar?.let { throw it }
            return productos.toList()
        }

        override suspend fun obtener(id: Long): Producto {
            return productos.first { it.id == id }
        }

        override suspend fun registrar(producto: Producto): Producto {
            throwOnRegistrar?.let { throw it }
            registrarLlamado = true
            val nuevo = producto.copy(id = (productos.size + 1).toLong())
            productos.add(nuevo)
            return nuevo
        }

        override suspend fun actualizar(producto: Producto): Producto {
            throwOnActualizar?.let { throw it }
            val index = productos.indexOfFirst { it.id == producto.id }
            if (index >= 0) productos[index] = producto
            return producto
        }

        override suspend fun eliminar(id: Long) {
            kotlinx.coroutines.delay(100)
            throwOnEliminar?.let { throw it }
            eliminarLlamadoId = id
            productos.removeAll { it.id == id }
        }
    }

    private fun crearViewModel(repo: FakeProductoRepository): ProductoViewModel {
        return ProductoViewModel(
            listarProductos = ListarProductosUseCase(repo),
            obtenerProducto = ObtenerProductoUseCase(repo),
            registrarProducto = RegistrarProductoUseCase(repo),
            actualizarProducto = ActualizarProductoUseCase(repo),
            eliminarProducto = EliminarProductoUseCase(repo)
        )
    }

    // 1. cargarProductos con lista no vacía cambia de Cargando a ConProductos
    @Test
    fun testCargarProductosConListaNoVaciaCambiaAConProductos() = runTest(testDispatcher) {
        val repo = FakeProductoRepository(
            productos = mutableListOf(
                Producto(id = 1L, nombre = "Paracetamol 500mg", precio = 3.5, stock = 50, activo = true)
            )
        )
        val viewModel = crearViewModel(repo)
        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is ProductoUiState.Fase.ConProductos)
        assertEquals(1, fase.productos.size)
        assertEquals("Paracetamol 500mg", fase.productos.first().nombre)
    }

    // 2. cargarProductos con lista vacía cambia a SinProductos
    @Test
    fun testCargarProductosConListaVaciaCambiaASinProductos() = runTest(testDispatcher) {
        val repo = FakeProductoRepository(productos = mutableListOf())
        val viewModel = crearViewModel(repo)
        advanceUntilIdle()

        assertEquals(ProductoUiState.Fase.SinProductos, viewModel.uiState.value.fase)
    }

    // 3. eliminar exitoso pasa por Operacion.EnCurso(Eliminar) y termina con Operacion.Inactiva recargando la lista
    @Test
    fun testEliminarExitosoPasaPorOperacionEnCursoYRecargaLista() = runTest(testDispatcher) {
        val prod = Producto(id = 10L, nombre = "Ibuprofeno 400mg", precio = 5.0, stock = 20, activo = true)
        val repo = FakeProductoRepository(productos = mutableListOf(prod))
        val viewModel = crearViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, (viewModel.uiState.value.fase as ProductoUiState.Fase.ConProductos).productos.size)

        viewModel.eliminar(10L)
        testScheduler.runCurrent()

        // Durante la ejecución en curso
        val opDurante = viewModel.uiState.value.operacion
        assertTrue(opDurante is ProductoUiState.Operacion.EnCurso)
        assertEquals(ProductoUiState.Operacion.Tipo.Eliminar, opDurante.tipo)

        advanceUntilIdle()

        assertEquals(10L, repo.eliminarLlamadoId)
        assertEquals(ProductoUiState.Operacion.Inactiva, viewModel.uiState.value.operacion)
        assertEquals("Producto eliminado correctamente", viewModel.uiState.value.mensajeExito)
        // Lista recargada está vacía
        assertEquals(ProductoUiState.Fase.SinProductos, viewModel.uiState.value.fase)
    }

    // 4. registrar con ErrorApi.Validacion (400) asigna el mensaje a los campos de formulario
    @Test
    fun testRegistrarConErrorApiValidacionAsignaErroresPorCampo() = runTest(testDispatcher) {
        val errorValidacion = ErrorApiException(
            ErrorApi.Validacion(
                porCampo = mapOf(
                    "nombre" to "El nombre debe tener al menos 3 caracteres",
                    "precio" to "El precio debe ser mayor que cero"
                )
            )
        )
        val repo = FakeProductoRepository(throwOnRegistrar = errorValidacion)
        val viewModel = crearViewModel(repo)
        advanceUntilIdle()

        viewModel.actualizarNombre("Abc") // Longitud >= 3 para pasar validación local
        viewModel.actualizarPrecio("10.0")
        viewModel.actualizarStock("5")
        viewModel.guardar()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("El nombre debe tener al menos 3 caracteres", state.formulario.nombreError)
        assertEquals("El precio debe ser mayor que cero", state.formulario.precioError)
        assertNull(state.formulario.stockError)
        assertEquals(ProductoUiState.Operacion.Inactiva, state.operacion)
    }

    // 5. Una falla de red mantiene Fase.ConProductos y establece Operacion.Fallida
    @Test
    fun testFallaDeRedMantieneListaVisibleYEstableceOperacionFallida() = runTest(testDispatcher) {
        val prod = Producto(id = 5L, nombre = "Amoxicilina 500mg", precio = 12.0, stock = 30, activo = true)
        val repo = FakeProductoRepository(
            productos = mutableListOf(prod),
            throwOnEliminar = ErrorApiException(ErrorApi.SinConexion)
        )
        val viewModel = crearViewModel(repo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.fase is ProductoUiState.Fase.ConProductos)

        viewModel.eliminar(5L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // La lista sigue visible sin destruirse
        assertTrue(state.fase is ProductoUiState.Fase.ConProductos)
        assertEquals(1, (state.fase as ProductoUiState.Fase.ConProductos).productos.size)
        // Operación pasó a Fallida
        assertTrue(state.operacion is ProductoUiState.Operacion.Fallida)
        assertTrue((state.operacion as ProductoUiState.Operacion.Fallida).mensaje.contains("No hay conexión"))
    }
}
