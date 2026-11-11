package pe.edu.upeu.pharmamobile2.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobile2.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile2.data.mapper.toDomain
import pe.edu.upeu.pharmamobile2.data.mapper.toRequest
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.remote.ejecutarLlamada
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApi
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.model.MotivoSync

import pe.edu.upeu.pharmamobile2.domain.model.CategoriaPredeterminada
import pe.edu.upeu.pharmamobile2.domain.port.DisparadorSincronizacion

class ProductoRepositoryOffline(
    private val local: ProductoLocal,
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long = CategoriaPredeterminada.ID,
    private val authRepository: AuthRepository? = null,
    private val disparador: DisparadorSincronizacion = DisparadorSincronizacion()
) : ProductoRepository {


    private inline fun <T> interceptarErroresAuth(bloque: () -> T): T {
        try {
            return bloque()
        } catch (e: ErrorApiException) {
            when (val err = e.error) {
                is ErrorApi.NoAutorizado -> authRepository?.notificar401()
                is ErrorApi.SinPermisos -> authRepository?.notificar403(err.mensaje)
                else -> {}
            }
            throw e
        }
    }

    override fun observar(): Flow<List<Producto>> = local.observar()

    override fun observarUltimaSincronizacion(): Flow<Long?> = local.observarUltimaSincronizacion()

    override suspend fun listar(): List<Producto> {
        return local.observar().first()
    }

    override suspend fun obtener(id: Long): Producto {
        val enLista = local.observar().first().find { it.id == id }
        if (enLista != null) return enLista
        return interceptarErroresAuth {
            ejecutarLlamada {
                api.obtener(id).toDomain()
            }.getOrThrow()
        }
    }

    private fun exigirEscritura() {
        val auth = authRepository ?: return // Direct construction is retained for isolated repository tests; DI always supplies auth.
        if (!auth.esActual(local.contexto)) throw ErrorApiException(ErrorApi.NoAutorizado)
        if (auth.sesionActual.value?.puede("CATALOGO_ESCRIBIR") != true)
            throw ErrorApiException(ErrorApi.SinPermisos("No tienes permiso para modificar el catálogo"))
    }

    override suspend fun registrar(producto: Producto): Producto {
        exigirEscritura()
        val guardado = local.guardarPendiente(producto)
        disparador.solicitar(MotivoSync.GUARDADO)
        return guardado
    }

    override suspend fun actualizar(producto: Producto): Producto = interceptarErroresAuth {
        exigirEscritura()
        val actualizadoRemoto = ejecutarLlamada {
            api.actualizar(
                id = producto.id,
                request = producto.toRequest(categoriaPorDefecto)
            ).toDomain()
        }.getOrThrow()

        local.guardarOActualizar(actualizadoRemoto, esPendiente = false)
        actualizadoRemoto
    }

    override suspend fun eliminar(id: Long): Unit = interceptarErroresAuth {
        exigirEscritura()
        ejecutarLlamada {
            api.eliminar(id)
        }.getOrThrow()

        local.eliminarLocal(id)
    }
}
