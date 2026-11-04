package pe.edu.upeu.pharmamobile2.domain.port

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import pe.edu.upeu.pharmamobile2.domain.model.*

interface SincronizadorProductos {
    val actividad: StateFlow<Boolean>
    val resultados: SharedFlow<ResumenSincronizacion>
    fun iniciar()
    suspend fun detener()
    suspend fun sincronizar(motivo: MotivoSync = MotivoSync.MANUAL): ResumenSincronizacion
    suspend fun reintentarDescarga(): ResumenSincronizacion
}
