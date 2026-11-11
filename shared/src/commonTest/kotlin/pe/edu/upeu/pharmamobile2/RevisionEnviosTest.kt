package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.test.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.usecase.RevisarEnvioUseCase
import pe.edu.upeu.pharmamobile2.testutil.*
import kotlin.test.*
import pe.edu.upeu.pharmamobile2.domain.port.DisparadorSincronizacion

class RevisionEnviosTest {
    private suspend fun fallo(f: EscenarioSync, naturaleza: NaturalezaFallo, codigo: String = "HTTP_503"): Long {
        val id=f.cola.guardar(Producto(0,"Prueba",1.0,1,categoriaId=26)).producto.id
        f.cola.iniciar(id,f.reloj.ahoraMs())
        f.cola.resolver(id,ResultadoEnvio.Fallo(FalloOperacion(naturaleza,codigo,"Fallo controlado",efectoIncierto=naturaleza==NaturalezaFallo.TRANSITORIO)),f.reloj.ahoraMs())
        return id
    }
    private fun revisar(f: EscenarioSync)=RevisarEnvioUseCase(f.cola,f.auth,f.contexto,f.reloj,DisparadorSincronizacion())
    @Test fun reenviarSinAceptarRiesgoNoLiberaNiHacePost()=runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val id=fallo(f,NaturalezaFallo.TRANSITORIO)
            f.reloj.instanteMs+=1000
            assertTrue(revisar(f)(id,DecisionEnvio.REENVIAR,false).isFailure)
            assertEquals(EstadoEnvio.REVISION,f.cola.porId(id)!!.estado)
            assertEquals(0,f.remoto.llamadas)
            assertTrue(revisar(f)(id,DecisionEnvio.REENVIAR,true).isSuccess)
            assertEquals(1,f.cola.porId(id)!!.intentos)
            f.enviar();assertEquals(1,f.remoto.llamadas)
        } finally { f.cerrar() }
    }
    @Test fun descartarSoloBorraLocalYNoPuedeBorrarUnEnvioEnCurso()=runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val id=fallo(f,NaturalezaFallo.TRANSITORIO)
            assertTrue(revisar(f)(id,DecisionEnvio.DESCARTAR).isSuccess)
            assertNull(f.cola.porId(id));assertEquals(0,f.remoto.llamadas)
            val enCurso=f.cola.guardar(Producto(0,"En curso",1.0,1)).producto.id
            f.cola.iniciar(enCurso,1000)
            assertTrue(revisar(f)(enCurso,DecisionEnvio.DESCARTAR).isFailure)
            assertNotNull(f.cola.porId(enCurso))
        } finally { f.cerrar() }
    }
    @Test fun corregirInciertaNoCambiaPayload()=runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val id=fallo(f,NaturalezaFallo.TRANSITORIO);val original=f.cola.porId(id)!!.producto
            assertTrue(revisar(f).corregir(id,original.copy(nombre="Cambiado")).isFailure)
            assertEquals(original,f.cola.porId(id)!!.producto)
        } finally { f.cerrar() }
    }
    @Test fun corregirRechazoConservaCamposYAbreNuevaRevision()=runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val id=fallo(f,NaturalezaFallo.PERMANENTE,"HTTP_400");val nuevo=f.cola.porId(id)!!.producto.copy(nombre="Corregido",precio=3.0,stock=8,categoriaId=27)
            assertTrue(revisar(f).corregir(id,nuevo).isSuccess)
            val despues=f.cola.porId(id)!!
            assertEquals(nuevo,despues.producto);assertEquals(1,despues.revision);assertEquals(0,despues.intentos)
            assertEquals(EstadoEnvio.PENDIENTE,despues.estado)
        } finally { f.cerrar() }
    }
    @Test fun quintoRechazoDeSesionRequiereRehabilitacionYNoQuedaAtrapado()=runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val id=fallo(f,NaturalezaFallo.SESION,"HTTP_401")
            repeat(4) {
                f.cola.autorizarReenvio(id,f.reloj.ahoraMs(),true);f.cola.iniciar(id,f.reloj.ahoraMs())
                f.cola.resolver(id,ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.SESION,"HTTP_401","Vuelve a iniciar sesión")),f.reloj.ahoraMs())
            }
            assertEquals(EstadoEnvio.FALLIDA_AGOTADA,f.cola.porId(id)!!.estado)
            assertTrue(revisar(f)(id,DecisionEnvio.REENVIAR,true).isFailure)
            assertTrue(revisar(f)(id,DecisionEnvio.REHABILITAR,true).isSuccess)
            assertEquals(1,f.cola.porId(id)!!.revision);assertEquals(0,f.cola.porId(id)!!.intentos)
        } finally { f.cerrar() }
    }
    @Test fun cuentaObsoletaNoPuedeDescartarPendientes()=runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val id=fallo(f,NaturalezaFallo.TRANSITORIO);f.auth.cerrarSesion()
            assertTrue(revisar(f)(id,DecisionEnvio.DESCARTAR).isFailure)
            assertNotNull(f.cola.porId(id))
        } finally { f.cerrar() }
    }
}
