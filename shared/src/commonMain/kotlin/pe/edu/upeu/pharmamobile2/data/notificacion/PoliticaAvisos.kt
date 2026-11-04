package pe.edu.upeu.pharmamobile2.data.notificacion

import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.model.ResumenSincronizacion

enum class DestinoAviso {
    LOGIN,
    REVISION_ACTUAL
}

object PoliticaAvisos {

    fun debeNotificar(
        anterior: ResumenSincronizacion?,
        actual: ResumenSincronizacion,
        habilitada: Boolean
    ): Boolean {
        if (!habilitada) return false
        if (anterior != null && anterior == actual) return false

        val huboEnviadas = actual.enviadas > (anterior?.enviadas ?: 0)
        val huboNuevasFallidas = actual.fallidas > (anterior?.fallidas ?: 0)
        val huboNuevasRevision = actual.revision > (anterior?.revision ?: 0)

        return huboEnviadas || huboNuevasFallidas || huboNuevasRevision
    }

    fun resolverDestino(
        cuentaAviso: ContextoCuenta,
        cuentaActiva: ContextoCuenta?
    ): DestinoAviso {
        return if (cuentaActiva != null &&
            cuentaAviso.clave == cuentaActiva.clave &&
            cuentaAviso.generacion == cuentaActiva.generacion
        ) {
            DestinoAviso.REVISION_ACTUAL
        } else {
            DestinoAviso.LOGIN
        }
    }
}
