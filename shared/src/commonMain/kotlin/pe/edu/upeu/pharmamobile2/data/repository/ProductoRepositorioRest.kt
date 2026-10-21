package pe.edu.upeu.pharmamobile2.data.repository

import pe.edu.upeu.pharmamobile2.data.mapper.toDomain
import pe.edu.upeu.pharmamobile2.data.mapper.toRequest
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

open class ProductoRepositorioRest(
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long = 26L,
    private val authRepository: pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository? = null
) : ProductoRepository {

    private inline fun <T> interceptarErroresAuth(bloque: () -> T): T {
        try {
            return bloque()
        } catch (e: pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException) {
            when (val err = e.error) {
                is pe.edu.upeu.pharmamobile2.domain.error.ErrorApi.NoAutorizado -> authRepository?.notificar401()
                is pe.edu.upeu.pharmamobile2.domain.error.ErrorApi.SinPermisos -> authRepository?.notificar403(err.mensaje)
                else -> {}
            }
            throw e
        }
    }

    override fun observar(): kotlinx.coroutines.flow.Flow<List<Producto>> = kotlinx.coroutines.flow.flow {
        emit(listar())
    }

    override suspend fun sincronizar() {
        listar()
    }

    override suspend fun listar(): List<Producto> = interceptarErroresAuth {
        ejecutarLlamada {
            api.listar().contenido.map { it.toDomain() }
        }.getOrThrow()
    }

    override suspend fun obtener(id: Long): Producto = interceptarErroresAuth {
        ejecutarLlamada {
            api.obtener(id).toDomain()
        }.getOrThrow()
    }

    override suspend fun registrar(producto: Producto): Producto = interceptarErroresAuth {
        ejecutarLlamada {
            api.crear(producto.toRequest(categoriaPorDefecto)).toDomain()
        }.getOrThrow()
    }

    override suspend fun actualizar(producto: Producto): Producto = interceptarErroresAuth {
        ejecutarLlamada {
            api.actualizar(
                id = producto.id,
                request = producto.toRequest(categoriaPorDefecto)
            ).toDomain()
        }.getOrThrow()
    }

    override suspend fun eliminar(id: Long): Unit = interceptarErroresAuth {
        ejecutarLlamada {
            api.eliminar(id)
        }.getOrThrow()
    }
}
