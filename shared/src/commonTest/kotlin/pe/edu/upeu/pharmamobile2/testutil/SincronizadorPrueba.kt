package pe.edu.upeu.pharmamobile2.testutil

import kotlinx.coroutines.flow.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.SincronizadorProductos

class SincronizadorPrueba(private val accion: suspend () -> Unit = {}) : SincronizadorProductos {
    override val actividad = MutableStateFlow(false)
    override val resultados = MutableSharedFlow<ResumenSincronizacion>()
    override fun iniciar() {}
    override suspend fun detener() {}
    override suspend fun sincronizar(motivo: MotivoSync): ResumenSincronizacion {
        accion(); return ResumenSincronizacion(0,0,0,0,0,true,null)
    }
    override suspend fun reintentarDescarga() = sincronizar(MotivoSync.REINTENTO_DESCARGA)
}
