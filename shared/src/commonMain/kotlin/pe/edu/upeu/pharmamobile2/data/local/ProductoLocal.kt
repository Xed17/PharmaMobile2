package pe.edu.upeu.pharmamobile2.data.local

import app.cash.sqldelight.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.db.Producto as ProductoEntity
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

import pe.edu.upeu.pharmamobile2.domain.model.EstadoEnvio

class ProductoLocal(
    private val db: PharmaDb,
    val contexto: ContextoCuenta,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    init { require(contexto.clave.isNotBlank() && contexto.clave != "legacy_unassigned") }
    private val queries = db.productoQueries
    private val metadatoQueries = db.metadatoQueries
    private val cuenta get() = contexto.clave

    fun observar(): Flow<List<Producto>> = queries.listarTodos(cuenta).asFlow().mapToList(dispatcher).map { rows -> rows.map { it.toDomain() } }.catch { if (it is java.sql.SQLException) emit(emptyList()) else throw it }
    fun listar(): List<Producto> = queries.listarTodos(cuenta).executeAsList().map { it.toDomain() }
    fun pendientes(): List<Producto> = queries.listarPendientes(cuenta).executeAsList().map { it.toDomain() }
    fun siguienteIdLocal(): Long = queries.siguienteIdLocal(cuenta).executeAsOneOrNull() ?: -1L

    @OptIn(ExperimentalUuidApi::class)
    fun guardarPendiente(producto: Producto): Producto = db.transactionWithResult {
        val pendiente = producto.copy(id = siguienteIdLocal())
        insertar(pendiente, true, Uuid.random().toString())
        pendiente
    }
    fun confirmarRegistro(localId: Long, remoto: Producto) {
        db.transaction {
            queries.eliminarPorId(cuenta, localId)
            insertar(remoto, false, null)
        }
    }
    fun reemplazarSincronizados(remotos: List<Producto>) {
        db.transaction {
            queries.borrarSincronizados(cuenta)
            remotos.forEach { insertar(it, false, null) }
        }
    }
    fun eliminarLocal(id: Long) { queries.eliminarPorId(cuenta, id) }
    @OptIn(ExperimentalUuidApi::class)
    fun guardarOActualizar(producto: Producto, esPendiente: Boolean = false) {
        insertar(producto, esPendiente, if(esPendiente) Uuid.random().toString() else null)
    }
    private fun insertar(p: Producto, pendiente: Boolean, operacion: String?) {
        queries.insertar(cuenta, p.id, p.nombre, p.precio, p.stock.toLong(), if(p.activo) 1L else 0L,
            p.categoriaId, p.categoriaNombre, if(pendiente) 1L else 0L,
            if(pendiente) EstadoEnvio.PENDIENTE.name else EstadoEnvio.CONFIRMADO.name, operacion)
    }
    fun contarPendientes(): Long = queries.contarPendientes(cuenta).executeAsOne()
    fun observarUltimaSincronizacion(): Flow<Long?> = metadatoQueries.obtener(cuenta, CLAVE_ULTIMA_SINC)
        .asFlow().mapToOneOrNull(dispatcher).map { it?.toLongOrNull() }.catch { if (it is java.sql.SQLException) emit(null) else throw it }
    fun obtenerUltimaSincronizacion(): Long? = metadatoQueries.obtener(cuenta, CLAVE_ULTIMA_SINC).executeAsOneOrNull()?.toLongOrNull()
    fun guardarUltimaSincronizacion(timestampMs: Long) { metadatoQueries.guardar(cuenta, CLAVE_ULTIMA_SINC, timestampMs.toString()) }
    private fun ProductoEntity.toDomain() = Producto(id, nombre, precio, stock.toInt(), activo == 1L, categoriaId ?: 0L, categoriaNombre)
    companion object { const val CLAVE_ULTIMA_SINC = "ultima_sincronizacion" }
}
