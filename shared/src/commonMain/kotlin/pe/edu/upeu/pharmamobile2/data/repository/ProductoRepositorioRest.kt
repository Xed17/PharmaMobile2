package pe.edu.upeu.pharmamobile2.data.repository

import pe.edu.upeu.pharmamobile2.data.mapper.toDomain
import pe.edu.upeu.pharmamobile2.data.mapper.toRequest
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

open class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long = 26L
) : ProductoRepository {

    override suspend fun listar(): List<Producto> =
        ejecutarLlamada {
            api.listar().contenido.map { it.toDomain() }
        }.getOrThrow()

    override suspend fun obtener(id: Long): Producto =
        ejecutarLlamada {
            api.obtener(id).toDomain()
        }.getOrThrow()

    override suspend fun registrar(producto: Producto): Producto =
        ejecutarLlamada {
            api.crear(producto.toRequest(categoriaPorDefecto)).toDomain()
        }.getOrThrow()

    override suspend fun actualizar(producto: Producto): Producto =
        ejecutarLlamada {
            api.actualizar(
                id = producto.id,
                request = producto.toRequest(categoriaPorDefecto)
            ).toDomain()
        }.getOrThrow()

    override suspend fun eliminar(id: Long) {
        ejecutarLlamada {
            api.eliminar(id)
        }.getOrThrow()
    }
}
