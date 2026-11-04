package pe.edu.upeu.pharmamobile2.data.notificacion

import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.model.ResumenSincronizacion
import pe.edu.upeu.pharmamobile2.domain.port.NotificadorSincronizacion

class NotificadorIos : NotificadorSincronizacion {
    override suspend fun publicar(contexto: ContextoCuenta, resumen: ResumenSincronizacion) {
        // Preparado para UNUserNotificationCenter en Mac
    }

    override suspend fun cancelar(contexto: ContextoCuenta) {
        // Preparado para UNUserNotificationCenter en Mac
    }
}
