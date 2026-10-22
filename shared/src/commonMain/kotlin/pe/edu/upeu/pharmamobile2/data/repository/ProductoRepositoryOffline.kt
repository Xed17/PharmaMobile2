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

class ProductoRepositoryOffline(
    private val local: ProductoLocal,
    private val api: ProductoApi,
    private val categoriaPorDefecto: Long = 26L,
    private val authRepository: AuthRepository? = null
) : ProductoRepository {

    private val sincronizacionMutex = Mutex()

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

    override suspend fun simularUltimaSincronizacionAntigua() {
        val hace26Horas = pe.edu.upeu.pharmamobile2.platform.obtenerTiempoActualMs() - (26L * 3600_000L)
        local.guardarUltimaSincronizacion(hace26Horas)
    }

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

    override suspend fun registrar(producto: Producto): Producto {
        // 1. Guardar primero en el almacenamiento local como pendiente con ID negativo
        val pendienteGuardado = local.guardarPendiente(producto)

        // 2. Intentar sincronización en segundo plano (no bloqueante / sin fallar si no hay red)
        try {
            sincronizar()
        } catch (_: Exception) {
            // Si no hay conexión o falla la sincronización, el producto ya se encuentra
            // seguro en la base local como pendiente y es visible de inmediato para el usuario.
        }

        return pendienteGuardado
    }

    override suspend fun sincronizar() = sincronizacionMutex.withLock {
        interceptarErroresAuth {
            // FASE 1: Subir productos pendientes ANTES de descargar
            val pendientes = local.pendientes()
            for (pendiente in pendientes) {
                try {
                    val respuesta = ejecutarLlamada {
                        api.crear(pendiente.toRequest(categoriaPorDefecto))
                    }.getOrThrow()

                    val confirmado = respuesta.toDomain()
                    // Reconciliación atómica: eliminar temporal y guardar confirmado
                    local.confirmarRegistro(localId = pendiente.id, remoto = confirmado)
                } catch (e: Exception) {
                    // Si ocurre un error durante la subida, se detiene la sincronización
                    // y el producto pendiente permanece protegido localmente para el próximo intento
                    throw e
                }
            }

            // FASE 2: Descargar catálogo remoto y reemplazar únicamente filas sincronizadas
            val remotos = ejecutarLlamada {
                api.obtenerProductos().map { it.toDomain() }
            }.getOrThrow()

            local.reemplazarSincronizados(remotos)
            local.guardarUltimaSincronizacion(pe.edu.upeu.pharmamobile2.platform.obtenerTiempoActualMs())
        }
    }

    override suspend fun actualizar(producto: Producto): Producto = interceptarErroresAuth {
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
        ejecutarLlamada {
            api.eliminar(id)
        }.getOrThrow()

        local.eliminarLocal(id)
    }
}
