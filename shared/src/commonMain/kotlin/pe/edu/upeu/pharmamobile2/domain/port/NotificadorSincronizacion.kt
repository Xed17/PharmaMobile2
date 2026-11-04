package pe.edu.upeu.pharmamobile2.domain.port

import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.model.ResumenSincronizacion

interface NotificadorSincronizacion {
    suspend fun publicar(contexto: ContextoCuenta, resumen: ResumenSincronizacion)
    suspend fun cancelar(contexto: ContextoCuenta)
}
