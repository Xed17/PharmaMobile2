package pe.edu.upeu.pharmamobile2.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobile2.domain.model.*

interface DescargaRepository {
    suspend fun actual(): CadenaDescarga?
    suspend fun guardar(cadena: CadenaDescarga)
    suspend fun conciliarCompleta(productos: List<Producto>, ahoraMs: Long)
    fun observarUltimaSync(): Flow<Long?>
}
