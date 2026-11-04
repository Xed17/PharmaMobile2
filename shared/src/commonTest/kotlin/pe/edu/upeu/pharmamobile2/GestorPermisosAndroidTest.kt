package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import pe.edu.upeu.pharmamobile2.data.permiso.GestorPermisosAndroid
import pe.edu.upeu.pharmamobile2.data.permiso.PuenteSolicitudPermiso
import pe.edu.upeu.pharmamobile2.data.permiso.ResultadoSolicitudAndroid
import pe.edu.upeu.pharmamobile2.domain.port.EstadoPermiso
import pe.edu.upeu.pharmamobile2.domain.port.Permiso
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GestorPermisosAndroidTest {

    private class DoblePuente(
        var resultado: ResultadoSolicitudAndroid = ResultadoSolicitudAndroid.Concedido,
        var rationale: Boolean = false,
        var conDemora: Boolean = false
    ) : PuenteSolicitudPermiso {
        var lanzamientos = 0
        var ajustesAbiertos = 0

        override suspend fun solicitar(permiso: String): ResultadoSolicitudAndroid {
            lanzamientos++
            if (conDemora) {
                delay(50)
            }
            return resultado
        }

        override fun debeMostrarJustificacion(permiso: String): Boolean = rationale

        override fun abrirAjustes() {
            ajustesAbiertos++
        }
    }

    @Test
    fun tablaDeEstadosYFlujoDePermisos() = runTest {
        var yaSolicitado = false
        var permisosConcedidosSO = false
        val puente = DoblePuente()

        val gestor = GestorPermisosAndroid(
            context = null,
            puente = puente,
            sdkInt = 33,
            estaConcedidoEnSO = { permisosConcedidosSO },
            yaSolicitado = { yaSolicitado },
            marcarSolicitado = { yaSolicitado = true }
        )

        // 1. Primer uso sin historial -> Denegado
        val primeraConsulta = gestor.estado(Permiso.NOTIFICACIONES)
        assertIs<EstadoPermiso.Denegado>(primeraConsulta)

        // 2. Cancelar diálogo no marca denegación definitiva
        puente.resultado = ResultadoSolicitudAndroid.Cancelado
        val despuesDeCancelar = gestor.solicitar(Permiso.NOTIFICACIONES)
        assertIs<EstadoPermiso.Denegado>(despuesDeCancelar)

        // 3. Denegación confirmada sin rationale -> DenegadoParaSiempre
        puente.resultado = ResultadoSolicitudAndroid.Rechazado
        puente.rationale = false
        val rechazoDefinitivo = gestor.solicitar(Permiso.NOTIFICACIONES)
        assertIs<EstadoPermiso.DenegadoParaSiempre>(rechazoDefinitivo)

        // 4. Concesión -> Concedido
        puente.resultado = ResultadoSolicitudAndroid.Concedido
        permisosConcedidosSO = true
        val concedido = gestor.solicitar(Permiso.NOTIFICACIONES)
        assertIs<EstadoPermiso.Concedido>(concedido)
    }

    @Test
    fun dosSolicitudesNoPierdenContinuacion() = runTest {
        val puente = DoblePuente(ResultadoSolicitudAndroid.Concedido, conDemora = true)
        val gestor = GestorPermisosAndroid(
            context = null,
            puente = puente,
            sdkInt = 33,
            estaConcedidoEnSO = { false },
            yaSolicitado = { false },
            marcarSolicitado = {}
        )

        val j1 = async { gestor.solicitar(Permiso.NOTIFICACIONES) }
        val j2 = async { gestor.solicitar(Permiso.NOTIFICACIONES) }

        val r1 = j1.await()
        val r2 = j2.await()

        assertEquals(1, puente.lanzamientos)
        assertIs<EstadoPermiso.Concedido>(r1)
        assertIs<EstadoPermiso.Concedido>(r2)
    }

    @Test
    fun api32NoSolicitaPostNotifications() = runTest {
        val puente = DoblePuente(ResultadoSolicitudAndroid.Concedido)
        val gestor = GestorPermisosAndroid(
            context = null,
            puente = puente,
            sdkInt = 32,
            estaConcedidoEnSO = { true },
            yaSolicitado = { false },
            marcarSolicitado = {}
        )

        val res = gestor.solicitar(Permiso.NOTIFICACIONES)
        assertIs<EstadoPermiso.Concedido>(res)
        assertEquals(0, puente.lanzamientos)
    }
}
