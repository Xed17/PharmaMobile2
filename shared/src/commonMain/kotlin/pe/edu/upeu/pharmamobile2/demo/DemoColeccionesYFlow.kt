package pe.edu.upeu.pharmamobile2.demo

import pe.edu.upeu.pharmamobile2.domain.filtrarProductosDisponibles
import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import pe.edu.upeu.pharmamobile2.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.model.mensajeEstado
import pe.edu.upeu.pharmamobile2.domain.obtenerMensajeProductoBuscado
import pe.edu.upeu.pharmamobile2.domain.obtenerNombresProductos
import pe.edu.upeu.pharmamobile2.domain.productosEjemplo
import pe.edu.upeu.pharmamobile2.domain.recolectarProductos

suspend fun ejecutarDemostracionCompleta() {
    println("PASO 1: CLIENTE Y NULL-SAFETY")
    val cliente1 = Cliente(1L, "Ana Torres", "ana@correo.com", "999888777")
    val cliente2 = Cliente(2L, "Luis Ramos", "luis@correo.com", null)
    println("Teléfono cliente 1: ${cliente1.obtenerTelefono()}")
    println("Teléfono cliente 2: ${cliente2.obtenerTelefono()}")

    println("\nPASO 2: PRODUCTO E INMUTABILIDAD (copy)")
    val prodOriginal = Producto(1, "Paracetamol", 5.0, 50)
    val prodActualizado = prodOriginal.copy(stock = 90)
    println("Original: $prodOriginal")
    println("Actualizado con copy(): $prodActualizado")

    println("\nPASO 3: OPERACIONES CON COLECCIONES")
    println("Productos disponibles (stock > 0): ${filtrarProductosDisponibles(productosEjemplo)}")
    println("Nombres de productos: ${obtenerNombresProductos(productosEjemplo)}")
    println("Buscar ID 2: ${obtenerMensajeProductoBuscado(2, productosEjemplo)}")
    println("Buscar ID 99: ${obtenerMensajeProductoBuscado(99, productosEjemplo)}")

    println("\nPASO 4: ESTADOS DE PEDIDO (sealed class y when)")
    val estados: List<EstadoPedido> = listOf(
        EstadoPedido.Pendiente,
        EstadoPedido.Procesando,
        EstadoPedido.Entregado,
        EstadoPedido.Rechazado("Stock insuficiente")
    )
    estados.forEach { estado ->
        println("Estado: ${mensajeEstado(estado)}")
    }

    println("\nPASO 5: ASINCRONÍA CON CORRUTINAS Y FLOW")
    println("Iniciando observación de productos reactivos...")
    recolectarProductos { productos ->
        println("-> Recibida emisión: ${productos.size} productos.")
    }
}
