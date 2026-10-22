package pe.edu.upeu.pharmamobile2.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobile2.domain.model.Producto

interface ProductoRepository {
    fun observar(): Flow<List<Producto>>
    fun observarUltimaSincronizacion(): Flow<Long?> = kotlinx.coroutines.flow.flowOf(null)
    suspend fun sincronizar()
    suspend fun simularUltimaSincronizacionAntigua() {}
    suspend fun listar(): List<Producto>
    suspend fun obtener(id: Long): Producto
    suspend fun registrar(producto: Producto): Producto
    suspend fun actualizar(producto: Producto): Producto
    suspend fun eliminar(id: Long)
}
