package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.presentation.producto.FaseProductos
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    // 1. Repositorio vacío produce FaseProductos.SinProductos
    @Test
    fun testRepositorioVacioProduceSinProductos() = runTest(testDispatcher) {
        val repoVacio = object : ProductoRepository {
            override suspend fun registrar(producto: Producto): Producto = producto
            override suspend fun listar(): List<Producto> = emptyList()
        }
        val useCase = RegistrarProductoUseCase(repoVacio)
        val viewModel = ProductoViewModel(useCase, repoVacio)

        advanceUntilIdle()

        assertEquals(FaseProductos.SinProductos, viewModel.uiState.value.fase)
    }

    // 2. Repositorio con productos produce FaseProductos.ConProductos
    @Test
    fun testRepositorioConProductosProduceConProductos() = runTest(testDispatcher) {
        val lista = listOf(
            Producto(id = 1, nombre = "Paracetamol", precio = 5.0, stock = 10, activo = true)
        )
        val repoConProductos = object : ProductoRepository {
            override suspend fun registrar(producto: Producto): Producto = producto
            override suspend fun listar(): List<Producto> = lista
        }
        val useCase = RegistrarProductoUseCase(repoConProductos)
        val viewModel = ProductoViewModel(useCase, repoConProductos)

        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is FaseProductos.ConProductos)
        assertEquals(1, fase.productos.size)
        assertEquals("Paracetamol", fase.productos.first().nombre)
    }

    // 3. Repositorio que lanza excepción produce FaseProductos.Error
    @Test
    fun testRepositorioConErrorProduceFaseError() = runTest(testDispatcher) {
        val repoConFallo = object : ProductoRepository {
            override suspend fun registrar(producto: Producto): Producto = throw RuntimeException("Error en servidor")
            override suspend fun listar(): List<Producto> = throw RuntimeException("Fallo de conexión")
        }
        val useCase = RegistrarProductoUseCase(repoConFallo)
        val viewModel = ProductoViewModel(useCase, repoConFallo)

        advanceUntilIdle()

        val fase = viewModel.uiState.value.fase
        assertTrue(fase is FaseProductos.Error)
        assertEquals("Fallo de conexión", fase.mensaje)
    }

    // 4. Registro con precio "0" deja mensaje de error en formulario sin llamar al repositorio
    @Test
    fun testRegistroConPrecioCeroDejaErrorSinLlamarAlRepositorio() = runTest(testDispatcher) {
        var registrarLlamado = false
        val repo = object : ProductoRepository {
            override suspend fun registrar(producto: Producto): Producto {
                registrarLlamado = true
                return producto
            }
            override suspend fun listar(): List<Producto> = emptyList()
        }
        val useCase = RegistrarProductoUseCase(repo)
        val viewModel = ProductoViewModel(useCase, repo)
        advanceUntilIdle()

        viewModel.actualizarNombre("Amoxicilina")
        viewModel.actualizarPrecio("0")
        viewModel.actualizarStock("15")
        viewModel.registrar()

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("El precio debe ser mayor que cero", state.errorPrecio)
        assertTrue(state.esErrorFormulario)
        assertNotNull(state.mensajeFormulario)
        assertTrue(!registrarLlamado, "El repositorio no debió ser invocado cuando el precio es 0")
    }
}
