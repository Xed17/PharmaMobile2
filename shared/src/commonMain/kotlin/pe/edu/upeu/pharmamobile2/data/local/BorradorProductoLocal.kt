package pe.edu.upeu.pharmamobile2.data.local

import app.cash.sqldelight.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.repository.BorradorProductoRepository

class BorradorProductoLocal(
    private val db: PharmaDb,
    contexto: ContextoCuenta,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : BorradorProductoRepository {
    private val cuenta = contexto.clave.also { require(it.isNotBlank() && it != "legacy_unassigned") }
    private val q = db.borradorProductoQueries
    override fun observar(): Flow<BorradorProducto?> = q.obtener(cuenta)
        .asFlow()
        .mapToOneOrNull(dispatcher)
        .map { row ->
            row?.let { BorradorProducto(it.idEdicion, it.nombre, it.precioTexto, it.stockTexto, it.categoriaId, it.categoriaNombre) }
        }
        .catch { e ->
            if (e is java.sql.SQLException) emit(null) else throw e
        }

    override suspend fun guardar(borrador: BorradorProducto) {
        db.transaction {
            val version = (q.obtener(cuenta).executeAsOneOrNull()?.version ?: 0) + 1
            q.guardar(cuenta, borrador.idEdicion, borrador.nombre, borrador.precioTexto, borrador.stockTexto, borrador.categoriaId, borrador.categoriaNombre, version)
        }
    }
    override suspend fun borrar() { q.borrar(cuenta) }
}
