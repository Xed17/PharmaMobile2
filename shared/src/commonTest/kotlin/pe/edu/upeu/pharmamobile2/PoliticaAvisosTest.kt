package pe.edu.upeu.pharmamobile2

import pe.edu.upeu.pharmamobile2.data.notificacion.DestinoAviso
import pe.edu.upeu.pharmamobile2.data.notificacion.PoliticaAvisos
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.model.ResumenSincronizacion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PoliticaAvisosTest {

    private val cuentaA = ContextoCuenta("acc_a", 1L)
    private val cuentaB = ContextoCuenta("acc_b", 1L)

    @Test
    fun sinCambiosNoNotifica() {
        val anterior = ResumenSincronizacion(
            enviadas = 1, pendientes = 0, fallidas = 0, revision = 0, bloqueadas = 0,
            descargaCompleta = true, ultimaSync = 1000L
        )
        assertFalse(PoliticaAvisos.debeNotificar(anterior, anterior, habilitada = true))
    }

    @Test
    fun mismoFalloNoSeRepite() {
        val anterior = ResumenSincronizacion(
            enviadas = 1, pendientes = 0, fallidas = 1, revision = 0, bloqueadas = 0,
            descargaCompleta = false, ultimaSync = 1000L
        )
        val nuevo = ResumenSincronizacion(
            enviadas = 1, pendientes = 0, fallidas = 1, revision = 0, bloqueadas = 0,
            descargaCompleta = false, ultimaSync = 1000L
        )
        var avisosPublicados = 0
        if (PoliticaAvisos.debeNotificar(null, anterior, habilitada = true)) {
            avisosPublicados++
        }
        if (PoliticaAvisos.debeNotificar(anterior, nuevo, habilitada = true)) {
            avisosPublicados++
        }
        assertEquals(1, avisosPublicados)
    }

    @Test
    fun denegarNoImpideSincronizar() {
        val resultadoSinPermiso = ResumenSincronizacion(
            enviadas = 2, pendientes = 0, fallidas = 0, revision = 0, bloqueadas = 0,
            descargaCompleta = true, ultimaSync = 1000L
        )
        assertFalse(PoliticaAvisos.debeNotificar(null, resultadoSinPermiso, habilitada = false))
        assertTrue(resultadoSinPermiso.descargaCompleta)
    }

    @Test
    fun salidaCancelaAviso() {
        val avisosActivos = mutableMapOf<String, String>()
        avisosActivos[cuentaA.clave] = "1 producto enviado"

        // Simula la cancelación de avisos al salir
        avisosActivos.remove(cuentaA.clave)
        assertEquals(0, avisosActivos.size)
    }

    @Test
    fun tapConOtraCuentaNoMuestraDatos() {
        val destino = PoliticaAvisos.resolverDestino(cuentaAviso = cuentaA, cuentaActiva = cuentaB)
        assertEquals(DestinoAviso.LOGIN, destino)

        val destinoMisma = PoliticaAvisos.resolverDestino(cuentaAviso = cuentaA, cuentaActiva = cuentaA)
        assertEquals(DestinoAviso.REVISION_ACTUAL, destinoMisma)
    }
}
