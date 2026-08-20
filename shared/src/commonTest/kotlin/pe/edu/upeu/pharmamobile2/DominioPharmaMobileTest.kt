package pe.edu.upeu.pharmamobile2

import pe.edu.upeu.pharmamobile2.domain.buscarProductoPorId
import pe.edu.upeu.pharmamobile2.domain.filtrarProductosDisponibles
import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import pe.edu.upeu.pharmamobile2.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.model.mensajeEstado
import pe.edu.upeu.pharmamobile2.domain.observarProductos
import pe.edu.upeu.pharmamobile2.domain.obtenerMensajeProductoBuscado
import pe.edu.upeu.pharmamobile2.domain.obtenerNombresProductos
import pe.edu.upeu.pharmamobile2.domain.productosEjemplo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DominioPharmaMobileTest {

    // 1. Paso 1: Cliente y null-safety
    @Test
    fun testClienteNullSafety() {
        val clienteConTel = Cliente(
            id = 1L,
            nombre = "Ana Torres",
            correo = "ana@correo.com",
            telefono = "999888777"
        )
        val clienteSinTel = Cliente(
            id = 2L,
            nombre = "Luis Ramos",
            correo = "luis@correo.com",
            telefono = null
        )

        assertEquals("999888777", clienteConTel.obtenerTelefono())
        assertEquals("No registrado", clienteSinTel.obtenerTelefono())
    }

    // 2. Paso 2: Producto, inmutabilidad y copy()
    @Test
    fun testProductoInmutabilidadYCopy() {
        val original = Producto(id = 1, nombre = "Paracetamol", precio = 5.0, stock = 50)
        val actualizado = original.copy(stock = 90)

        assertEquals(50, original.stock)
        assertEquals(90, actualizado.stock)
        assertEquals("Paracetamol", actualizado.nombre)

        val disminuido = original.disminuirStock(10)
        assertEquals(40, disminuido.stock)
        assertEquals(50, original.stock)
    }

    // 3. Paso 3: Operaciones con colecciones (filter, map, find)
    @Test
    fun testOperacionesColecciones() {
        // Filter
        val disponibles = filtrarProductosDisponibles(productosEjemplo)
        assertEquals(2, disponibles.size)
        assertEquals(listOf("Paracetamol", "Amoxicilina"), disponibles.map { it.nombre })

        // Map
        val nombres = obtenerNombresProductos(productosEjemplo)
        assertEquals(listOf("Paracetamol", "Ibuprofeno", "Amoxicilina"), nombres)

        // Find existente
        val encontrado = buscarProductoPorId(2, productosEjemplo)
        assertNotNull(encontrado)
        assertEquals("Ibuprofeno", encontrado.nombre)
        assertEquals("Ibuprofeno", obtenerMensajeProductoBuscado(2, productosEjemplo))

        // Find inexistente
        val noEncontrado = buscarProductoPorId(99, productosEjemplo)
        assertNull(noEncontrado)
        assertEquals("Producto no encontrado", obtenerMensajeProductoBuscado(99, productosEjemplo))
    }

    // 4. Paso 4: EstadoPedido y evaluación con when (mensajeEstado)
    @Test
    fun testEstadoPedidoWhen() {
        val pendiente: EstadoPedido = EstadoPedido.Pendiente
        val procesando: EstadoPedido = EstadoPedido.Procesando
        val entregado: EstadoPedido = EstadoPedido.Entregado
        val rechazado: EstadoPedido = EstadoPedido.Rechazado("Stock insuficiente")

        assertEquals("El pedido está pendiente", mensajeEstado(pendiente))
        assertEquals("El pedido se está procesando", mensajeEstado(procesando))
        assertEquals("El pedido ha sido entregado", mensajeEstado(entregado))
        assertEquals("Pedido rechazado: Stock insuficiente", mensajeEstado(rechazado))
    }

    // 5. Paso 5: Flow de productos
    @Test
    fun testObservarProductosFlow() {
        val flow = observarProductos()
        assertNotNull(flow)
    }
}
