package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobile2.data.local.DescargaLocal
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.usecase.DescargarCatalogoUseCase
import pe.edu.upeu.pharmamobile2.testutil.*
import kotlin.test.*

class DescargaCatalogoTest {
    private fun fallo() = Result.failure<List<Producto>>(FalloOperacionException(FalloOperacion(NaturalezaFallo.TRANSITORIO,"HTTP_503","Servicio no disponible")))
    private fun caso(f: EscenarioSync, espera: EsperaFalsa, local: DescargaLocal)=DescargarCatalogoUseCase(local,f.remoto,f.auth,f.contexto,f.reloj,espera,GeneradorIdsSecuencial())

    @Test fun cincoRecorridosConEsperaCrecienteConservanCacheYFecha() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val espera=EsperaFalsa()
        try {
            val local=DescargaLocal(f.db,f.contexto)
            f.local.guardarOActualizar(Producto(id=44L,nombre="Original",precio=2.0,stock=1))
            f.local.guardarUltimaSincronizacion(500)
            f.remoto.descarga={ fallo() }
            assertFalse(caso(f,espera,local)())
            assertEquals(5,f.remoto.lecturas)
            assertEquals(listOf(1000L,2000L,4000L,8000L),espera.demoras)
            assertEquals(EstadoCadena.AGOTADA,local.actual()!!.estado)
            assertEquals(listOf(44L),f.local.listar().map { it.id })
            assertEquals(500L,local.observarUltimaSync().first())
        } finally { f.cerrar() }
    }
    @Test fun agotadaRequiereAccionExplicitaYPlazoDeDieciseisSegundos() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val espera=EsperaFalsa()
        try {
            val local=DescargaLocal(f.db,f.contexto);val descargar=caso(f,espera,local)
            f.remoto.descarga={ fallo() };descargar()
            f.remoto.descarga={ Result.success(emptyList()) }
            assertFalse(descargar())
            f.reloj.instanteMs+=15999
            assertFalse(descargar(reiniciarAgotada=true))
            f.reloj.instanteMs++
            assertTrue(descargar(reiniciarAgotada=true))
            assertEquals(6,f.remoto.lecturas)
            assertEquals(EstadoCadena.COMPLETA,local.actual()!!.estado)
        } finally { f.cerrar() }
    }
    @Test fun reiniciarNoRestablecePresupuestoPersistido() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val espera=EsperaFalsa()
        try {
            val local=DescargaLocal(f.db,f.contexto)
            local.guardar(CadenaDescarga("anterior",EstadoCadena.ESPERA,3,1000,null))
            f.remoto.descarga={ fallo() }
            assertFalse(caso(f,espera,local)())
            assertEquals(2,f.remoto.lecturas)
            assertEquals(listOf(8000L),espera.demoras)
            assertEquals("anterior",local.actual()!!.id)
            assertEquals(5,local.actual()!!.intentos)
        } finally { f.cerrar() }
    }
    @Test fun cancelarNoBorraCacheNiFingeExito() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val espera=EsperaFalsa()
        try {
            val local=DescargaLocal(f.db,f.contexto)
            f.local.guardarOActualizar(Producto(id=44L,nombre="Original",precio=2.0,stock=1))
            f.remoto.descarga={ throw CancellationException("prueba") }
            assertFailsWith<CancellationException> { caso(f,espera,local)() }
            assertEquals(EstadoCadena.EN_CURSO,local.actual()!!.estado)
            assertEquals(listOf(44L),f.local.listar().map { it.id })
            assertNull(local.observarUltimaSync().first())
            f.remoto.descarga={ Result.success(emptyList()) }
            assertTrue(caso(f,espera,local)())
            assertEquals(2,local.actual()!!.intentos)
        } finally { f.cerrar() }
    }
    @Test fun esperaReanudadaNoBloqueaIndefinidamenteTrasCambiarReloj() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val espera=EsperaFalsa()
        try {
            val local=DescargaLocal(f.db,f.contexto)
            local.guardar(CadenaDescarga("anterior",EstadoCadena.ESPERA,3,999999,null))
            assertTrue(caso(f,espera,local)())
            assertEquals(listOf(16000L),espera.demoras)
            assertEquals(4,local.actual()!!.intentos)
        } finally { f.cerrar() }
    }
    @Test fun errorPermanenteNoSeReintentaAutomaticamente() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler));val espera=EsperaFalsa()
        try {
            val local=DescargaLocal(f.db,f.contexto)
            f.remoto.descarga={ Result.failure(FalloOperacionException(FalloOperacion(NaturalezaFallo.PERMANENTE,"DATOS_INVALIDOS","Datos inválidos"))) }
            val descargar=caso(f,espera,local)
            assertFalse(descargar());assertFalse(descargar())
            assertEquals(1,f.remoto.lecturas)
            assertTrue(espera.demoras.isEmpty())
            assertEquals(EstadoCadena.BLOQUEADA,local.actual()!!.estado)
        } finally { f.cerrar() }
    }
}
