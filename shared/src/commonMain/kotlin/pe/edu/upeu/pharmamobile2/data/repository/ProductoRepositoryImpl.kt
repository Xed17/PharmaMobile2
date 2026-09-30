package pe.edu.upeu.pharmamobile2.data.repository

import pe.edu.upeu.pharmamobile2.data.mapper.toDomain
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class ProductoRepositoryImpl(
    private val api: ProductoApi
) : ProductoRepository {

    private val productosLocales = mutableListOf<Producto>()
    private var siguienteIdLocal = 1000

    override suspend fun registrar(producto: Producto): Producto {
        val registrado = producto.copy(id = if (producto.id > 0) producto.id else siguienteIdLocal++)
        productosLocales.add(0, registrado)
        return registrado
    }

    override suspend fun listar(): List<Producto> {
        val remotos = api.obtenerProductos().map { it.toDomain() }
        return productosLocales + remotos
    }
}
