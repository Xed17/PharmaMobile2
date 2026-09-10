package pe.edu.upeu.pharmamobile2.data.repository

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class ProductoRepositorioEnMemoria : ProductoRepository {
    private val productos = mutableListOf(
        Producto(id = 1, nombre = "Paracetamol", precio = 15.50, stock = 100, activo = true),
        Producto(id = 2, nombre = "Ibuprofeno", precio = 18.90, stock = 50, activo = true),
        Producto(id = 3, nombre = "Amoxicilina", precio = 25.00, stock = 5, activo = true),
        Producto(id = 4, nombre = "Loratadina", precio = 12.50, stock = 0, activo = false),
        Producto(id = 5, nombre = "Diclofenaco", precio = 20.00, stock = 3, activo = true)
    )
    private var siguienteId = 6

    override suspend fun registrar(producto: Producto): Producto {
        delay(500)
        val registrado = producto.copy(id = siguienteId++)
        productos += registrado
        return registrado
    }

    override suspend fun listar(): List<Producto> {
        delay(500)
        return productos.toList()
    }
}
