package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import pe.edu.upeu.pharmamobile2.data.local.DescargaLocal
import pe.edu.upeu.pharmamobile2.data.sync.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.*
import pe.edu.upeu.pharmamobile2.domain.usecase.DescargarCatalogoUseCase
import pe.edu.upeu.pharmamobile2.testutil.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class CoordinadorSincronizacionTest {
    private fun crear(f: EscenarioSync,scope: CoroutineScope,monitor: MonitorFalso=MonitorFalso()): CoordinadorSincronizacion {
        val descarga=DescargaLocal(f.db,f.contexto)
        val descargar=DescargarCatalogoUseCase(descarga,f.remoto,f.auth,f.contexto,f.reloj,EsperaFalsa(),GeneradorIdsSecuencial())
        return CoordinadorSincronizacion(scope,f.enviar,descargar,f.cola,descarga,f.auth,f.contexto,DisparadorSincronizacion(),monitor)
    }
    @Test fun subeAntesDeDescargarYResumeSoloSuCuenta() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val c=crear(f,backgroundScope)
        try {
            val orden=mutableListOf<String>()
            f.cola.guardar(Producto(nombre="Prueba",precio=2.0,stock=1))
            f.remoto.respuesta={ orden.add("POST");ResultadoEnvio.Exito(it.copy(id=45L)) }
            f.remoto.descarga={ orden.add("GET");Result.success(listOf(Producto(id=45L,nombre="Prueba",precio=2.0,stock=1))) }
            val resumen=c.sincronizar(MotivoSync.MANUAL)
            assertEquals(listOf("POST","GET"),orden)
            assertEquals(1,resumen.enviadas);assertEquals(0,resumen.pendientes)
            assertTrue(resumen.descargaCompleta)
            assertFalse(c.actividad.value)
        } finally { c.detener();f.cerrar() }
    }
    @Test fun sinConexionNoConsumeIntentosDeEnvioNiDescarga() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val c=crear(f,backgroundScope,MonitorFalso(EstadoConectividad.NO_DISPONIBLE))
        try {
            val id=f.cola.guardar(Producto(nombre="Prueba",precio=2.0,stock=1)).producto.id
            val resumen=c.sincronizar(MotivoSync.MANUAL)
            assertEquals(1,resumen.pendientes);assertFalse(resumen.descargaCompleta)
            assertEquals(0,f.remoto.llamadas);assertEquals(0,f.remoto.lecturas)
            assertEquals(0,f.cola.porId(id)!!.intentos)
        } finally { c.detener();f.cerrar() }
    }
    @Test fun pulsacionesConcurrentesSeAgrupanEnUnaPasadaPosterior() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val c=crear(f,backgroundScope)
        val iniciada=CompletableDeferred<Unit>();val liberar=CompletableDeferred<Unit>()
        try {
            f.remoto.descarga={ if(f.remoto.lecturas==1) { iniciada.complete(Unit);liberar.await() };Result.success(emptyList()) }
            val llamadas=(0 until 20).map { async { c.sincronizar(MotivoSync.MANUAL) } }
            iniciada.await();runCurrent();liberar.complete(Unit);llamadas.awaitAll()
            assertEquals(2,f.remoto.lecturas)
            assertFalse(c.actividad.value)
        } finally { liberar.complete(Unit);c.detener();f.cerrar() }
    }
    @Test fun reintentarDescargaNoLiberaCreacionesInciertas() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val c=crear(f,backgroundScope)
        try {
            val id=f.cola.guardar(Producto(nombre="Prueba",precio=2.0,stock=1)).producto.id
            f.cola.iniciar(id,1000)
            f.cola.resolver(id,ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.TRANSITORIO,"TIMEOUT","Tiempo agotado",efectoIncierto=true)),1000)
            f.remoto.descarga={ Result.failure(FalloOperacionException(FalloOperacion(NaturalezaFallo.TRANSITORIO,"HTTP_503","Servicio no disponible"))) }
            c.sincronizar(MotivoSync.MANUAL)
            f.reloj.instanteMs+=16000
            f.remoto.descarga={ Result.success(emptyList()) }
            c.sincronizar(MotivoSync.MANUAL)
            assertEquals(5,f.remoto.lecturas)
            assertTrue(c.reintentarDescarga().descargaCompleta)
            assertEquals(6,f.remoto.lecturas)
            assertEquals(EstadoEnvio.REVISION,f.cola.porId(id)!!.estado)
            assertEquals(0,f.remoto.llamadas)
        } finally { c.detener();f.cerrar() }
    }
    @Test fun perderRedCancelaCicloYReconexionReanudaLecturaSegura() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val monitor=MonitorFalso();val c=crear(f,backgroundScope,monitor)
        val iniciada=CompletableDeferred<Unit>();val liberar=CompletableDeferred<Unit>()
        try {
            f.remoto.descarga={ if(f.remoto.lecturas==1) { iniciada.complete(Unit);liberar.await() };Result.success(emptyList()) }
            c.iniciar();iniciada.await()
            monitor.estado.value=EstadoConectividad.NO_DISPONIBLE;runCurrent()
            assertFalse(c.actividad.value)
            assertEquals(EstadoCadena.EN_CURSO,DescargaLocal(f.db,f.contexto).actual()!!.estado)
            val resultado=async { c.resultados.first { it.descargaCompleta } }
            runCurrent();monitor.estado.value=EstadoConectividad.DISPONIBLE
            assertTrue(resultado.await().descargaCompleta)
            assertEquals(2,DescargaLocal(f.db,f.contexto).actual()!!.intentos)
        } finally { liberar.complete(Unit);c.detener();f.cerrar() }
    }
}
