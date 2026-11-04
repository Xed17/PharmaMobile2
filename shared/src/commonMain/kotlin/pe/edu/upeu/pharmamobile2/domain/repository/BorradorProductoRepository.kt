package pe.edu.upeu.pharmamobile2.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobile2.domain.model.*

interface BorradorProductoRepository {
    fun observar(): Flow<BorradorProducto?>
    suspend fun guardar(borrador: BorradorProducto)
    suspend fun borrar()
}
