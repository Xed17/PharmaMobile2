package pe.edu.upeu.pharmamobile2.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.db.Producto as ProductoEntity
import pe.edu.upeu.pharmamobile2.domain.model.Producto

class ProductoLocal(
    private val db: PharmaDb,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val queries = db.productoQueries

    fun observar(): Flow<List<Producto>> =
        queries.listarTodos()
            .asFlow()
            .mapToList(dispatcher)
            .map { list -> list.map { it.toDomain() } }

    fun pendientes(): List<Producto> =
        queries.listarPendientes()
            .executeAsList()
            .map { it.toDomain() }

    fun siguienteIdLocal(): Long {
        val siguiente = queries.siguienteIdLocal().executeAsOneOrNull()
        return siguiente ?: -1L
    }

    fun guardarPendiente(producto: Producto): Producto {
        val idLocal = siguienteIdLocal()
        val pendiente = producto.copy(id = idLocal)
        queries.insertar(
            id = pendiente.id,
            nombre = pendiente.nombre,
            precio = pendiente.precio,
            stock = pendiente.stock.toLong(),
            pendiente = 1L
        )
        return pendiente
    }

    fun confirmarRegistro(localId: Long, remoto: Producto) {
        db.transaction {
            queries.eliminarPorId(localId)
            queries.insertar(
                id = remoto.id,
                nombre = remoto.nombre,
                precio = remoto.precio,
                stock = remoto.stock.toLong(),
                pendiente = 0L
            )
        }
    }

    fun reemplazarSincronizados(remotos: List<Producto>) {
        db.transaction {
            queries.borrarSincronizados()
            remotos.forEach { r ->
                queries.insertar(
                    id = r.id,
                    nombre = r.nombre,
                    precio = r.precio,
                    stock = r.stock.toLong(),
                    pendiente = 0L
                )
            }
        }
    }

    fun eliminarLocal(id: Long) {
        queries.eliminarPorId(id)
    }

    fun guardarOActualizar(producto: Producto, esPendiente: Boolean = false) {
        queries.insertar(
            id = producto.id,
            nombre = producto.nombre,
            precio = producto.precio,
            stock = producto.stock.toLong(),
            pendiente = if (esPendiente) 1L else 0L
        )
    }

    fun contarPendientes(): Long = queries.contarPendientes().executeAsOne()

    private fun ProductoEntity.toDomain(): Producto = Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock.toInt(),
        activo = true,
        categoriaId = 26L,
        categoriaNombre = "Analgésicos"
    )
}
