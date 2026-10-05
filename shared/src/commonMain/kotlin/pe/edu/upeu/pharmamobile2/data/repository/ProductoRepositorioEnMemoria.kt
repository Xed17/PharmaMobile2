package pe.edu.upeu.pharmamobile2.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class ProductoRepositorioEnMemoria : ProductoRepository {
    private val productos = mutableListOf(
        Producto(id = 1L, nombre = "Paracetamol", precio = 15.50, stock = 100, activo = true),
        Producto(id = 2L, nombre = "Ibuprofeno", precio = 18.90, stock = 50, activo = true),
        Producto(id = 3L, nombre = "Amoxicilina", precio = 25.00, stock = 5, activo = true),
        Producto(id = 4L, nombre = "Loratadina", precio = 12.50, stock = 0, activo = false),
        Producto(id = 5L, nombre = "Diclofenaco", precio = 20.00, stock = 3, activo = true)
    )
    private var siguienteId = 6L

    override suspend fun registrar(producto: Producto): Producto {
        delay(300)
        val registrado = producto.copy(id = if (producto.id > 0L) producto.id else siguienteId++)
        productos += registrado
        return registrado
    }

    override suspend fun listar(): List<Producto> {
        delay(300)
        return productos.toList()
    }

    override suspend fun obtener(id: Long): Producto {
        delay(200)
        return productos.first { it.id == id }
    }

    override suspend fun actualizar(producto: Producto): Producto {
        delay(300)
        val index = productos.indexOfFirst { it.id == producto.id }
        if (index >= 0) productos[index] = producto
        return producto
    }

    override suspend fun eliminar(id: Long) {
        delay(300)
        productos.removeAll { it.id == id }
    }
}
