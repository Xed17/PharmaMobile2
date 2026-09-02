package pe.edu.upeu.pharmamobile2.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import pe.edu.upeu.pharmamobile2.domain.model.DetallePedido
import pe.edu.upeu.pharmamobile2.domain.model.EstadoPedido
import pe.edu.upeu.pharmamobile2.domain.model.Pedido
import pe.edu.upeu.pharmamobile2.domain.model.Producto

object InMemoryRepository {

    val productos: SnapshotStateList<Producto> = mutableStateListOf(
        Producto(id = 1, nombre = "Paracetamol 500mg", precio = 8.50, stock = 100),
        Producto(id = 2, nombre = "Ibuprofeno 400mg", precio = 12.00, stock = 50),
        Producto(id = 3, nombre = "Amoxicilina 500mg", precio = 18.50, stock = 30),
        Producto(id = 4, nombre = "Loratadina 10mg", precio = 10.00, stock = 40)
    )

    val clientes: SnapshotStateList<Cliente> = mutableStateListOf(
        Cliente(id = 1L, nombre = "Juan Pérez", correo = "juan.perez@gmail.com", telefono = "987654321"),
        Cliente(id = 2L, nombre = "María Gómez", correo = "maria.gomez@outlook.com", telefono = "912345678"),
        Cliente(id = 3L, nombre = "Carlos Mendoza", correo = "carlos.mendoza@yahoo.com", telefono = "955443322")
    )

    val pedidos: SnapshotStateList<Pedido> = mutableStateListOf()

    private var nextProductoId = 5
    private var nextClienteId = 4L
    private var nextPedidoId = 1

    fun agregarProducto(producto: Producto) {
        val nuevoProducto = if (producto.id <= 0) {
            producto.copy(id = nextProductoId++)
        } else {
            producto
        }
        productos.add(nuevoProducto)
    }

    fun agregarCliente(cliente: Cliente) {
        val nuevoCliente = if (cliente.id <= 0L) {
            cliente.copy(id = nextClienteId++)
        } else {
            cliente
        }
        clientes.add(nuevoCliente)
    }

    fun registrarPedido(cliente: Cliente, producto: Producto, cantidad: Int): Pedido {
        // Descontar el stock del producto
        val productoActualizado = producto.disminuirStock(cantidad)
        val index = productos.indexOfFirst { it.id == producto.id }
        if (index != -1) {
            productos[index] = productoActualizado
        }

        val detalle = DetallePedido(producto = productoActualizado, cantidad = cantidad)
        val nuevoPedido = Pedido(
            id = nextPedidoId++,
            cliente = cliente,
            detalles = listOf(detalle),
            estado = EstadoPedido.Pendiente
        )
        pedidos.add(nuevoPedido)
        return nuevoPedido
    }
}
