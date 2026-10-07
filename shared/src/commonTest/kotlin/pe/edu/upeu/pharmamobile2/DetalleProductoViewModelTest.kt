package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobile2.platform.formatearSoles
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.producto.toUi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DetalleProductoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class CompartidorFake : Compartidor {
        var ultimoTexto: String? = null
        var vecesLlamado = 0

        override fun compartir(texto: String) {
            ultimoTexto = texto
            vecesLlamado++
        }
    }

    private class FakeRepo(
        val productos: List<Producto> = listOf()
    ) : ProductoRepository {
        override suspend fun listar(): List<Producto> = productos
        override suspend fun obtener(id: Long): Producto = productos.first { it.id == id }
        override suspend fun registrar(producto: Producto): Producto = producto
        override suspend fun actualizar(producto: Producto): Producto = producto
        override suspend fun eliminar(id: Long) {}
    }

    @Test
    fun testFormatearSolesDevuelveSimboloYValor() {
        val resultado = formatearSoles(12.50)
        assertNotNull(resultado)
        assertTrue(
            resultado.contains("S/") || resultado.contains("PEN") || resultado.contains("S/.") || resultado.contains("12.5"),
            "El formato debe contener el valor o símbolo: $resultado"
        )
    }

    @Test
    fun testComoTextoParaCompartirIncluyeNombrePrecioYStock() {
        val prod = Producto(
            id = 1L,
            nombre = "Paracetamol 500mg",
            precio = 12.50,
            stock = 35
        )
        val texto = prod.comoTextoParaCompartir()
        assertTrue(texto.contains("Paracetamol 500mg"), "Debe contener el nombre")
        assertTrue(texto.contains("Stock: 35"), "Debe contener el stock")
        assertTrue(texto.contains(formatearSoles(12.50)), "Debe contener el precio formateado")
    }

    @Test
    fun testDetalleViewModelCompartirEnviaTextoACompartidor() = runTest(testDispatcher) {
        val prod = Producto(
            id = 5L,
            nombre = "Amoxicilina 500mg",
            precio = 18.00,
            stock = 20
        )
        val repo = FakeRepo(listOf(prod))
        val compartidorFake = CompartidorFake()
        val viewModel = DetalleProductoViewModel(
            repository = repo,
            compartidor = compartidorFake
        )

        viewModel.establecerProducto(prod)
        viewModel.compartir()

        assertEquals(1, compartidorFake.vecesLlamado)
        assertEquals(prod.comoTextoParaCompartir(), compartidorFake.ultimoTexto)
    }

    @Test
    fun testDetalleViewModelCargarActualizaEstadoConProductoUi() = runTest(testDispatcher) {
        val prod = Producto(
            id = 10L,
            nombre = "Ibuprofeno 400mg",
            precio = 8.50,
            stock = 15
        )
        val repo = FakeRepo(listOf(prod))
        val compartidorFake = CompartidorFake()
        val viewModel = DetalleProductoViewModel(
            repository = repo,
            compartidor = compartidorFake
        )

        viewModel.cargar(10L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.cargando)
        val producto = state.producto
        assertNotNull(producto)
        assertEquals("Ibuprofeno 400mg", producto.nombre)
        assertEquals(formatearSoles(8.50), producto.precio)
    }

    @Test
    fun testProductoToUiMapeaPrecioFormateado() {
        val producto = Producto(
            id = 1L,
            nombre = "Aspirina 100mg",
            precio = 5.0,
            stock = 40
        )
        val ui = producto.toUi()
        assertEquals(1L, ui.id)
        assertEquals("Aspirina 100mg", ui.nombre)
        assertEquals(formatearSoles(5.0), ui.precio)
        assertEquals(40, ui.stock)
    }
}
