package pe.edu.upeu.pharmamobile2.domain.repository

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobile2.domain.model.*

interface ColaProductoRepository {
    fun observar(): Flow<List<RegistroEnvio>>
    suspend fun elegibles(): List<RegistroEnvio>
    suspend fun porId(id: Long): RegistroEnvio?
    suspend fun guardar(producto: Producto): RegistroEnvio
    suspend fun iniciar(id: Long, ahoraMs: Long): RegistroEnvio?
    suspend fun resolver(id: Long, resultado: ResultadoEnvio, ahoraMs: Long)
    suspend fun recuperarInterrumpidos()
    suspend fun autorizarReenvio(id: Long, ahoraMs: Long, aceptaRiesgo: Boolean)
    suspend fun corregirRechazado(id: Long, producto: Producto)
    suspend fun rehabilitarAgotado(id: Long, ahoraMs: Long, aceptaRiesgo: Boolean)
    suspend fun descartarLocal(id: Long)
}
