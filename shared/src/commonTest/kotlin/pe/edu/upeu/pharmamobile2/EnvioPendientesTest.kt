package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.testutil.*
import kotlin.test.*

class EnvioPendientesTest {
    @Test fun rechazoDeUnaFilaNoBloqueaLaSiguiente() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val mala=f.cola.guardar(Producto(nombre="Rechazo",precio=2.0,stock=1))
            f.cola.guardar(Producto(nombre="Valida",precio=3.0,stock=2))
            f.remoto.respuesta={ if(it.nombre=="Rechazo") ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.PERMANENTE,"HTTP_409","Revisar conflicto")) else ResultadoEnvio.Exito(it.copy(id=45L)) }
            val resumen=f.enviar()
            assertEquals(1,resumen.enviadas)
            assertEquals(EstadoEnvio.FALLIDA_PERMANENTE,f.cola.porId(mala.producto.id)!!.estado)
            assertEquals(setOf(-1L,45L),f.local.listar().map { it.id }.toSet())
        } finally { f.cerrar() }
    }
    @Test fun inciertaNoSeVuelveAEnviarEnOtroCiclo() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val id=f.cola.guardar(Producto(nombre="Prueba",precio=2.0,stock=1)).producto.id
            f.remoto.respuesta={ ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.TRANSITORIO,"TIMEOUT","Tiempo agotado",efectoIncierto=true)) }
            f.enviar();f.enviar()
            assertEquals(1,f.remoto.llamadas)
            assertEquals(EstadoEnvio.REVISION,f.cola.porId(id)!!.estado)
            assertEquals(1,f.cola.porId(id)!!.intentos)
        } finally { f.cerrar() }
    }
    @Test fun cancelacionConservaIntentoYNoConsumeLaSiguienteFila() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val primero=f.cola.guardar(Producto(nombre="Uno",precio=2.0,stock=1)).producto.id
            val segundo=f.cola.guardar(Producto(nombre="Dos",precio=2.0,stock=1)).producto.id
            f.remoto.respuesta={ throw CancellationException("prueba") }
            assertFailsWith<CancellationException> { f.enviar() }
            assertEquals(EstadoEnvio.ENVIANDO,f.cola.porId(primero)!!.estado)
            assertEquals(0,f.cola.porId(segundo)!!.intentos)
            f.cola.recuperarInterrumpidos()
            assertEquals(EstadoEnvio.REVISION,f.cola.porId(primero)!!.estado)
        } finally { f.cerrar() }
    }
    @Test fun rechazo401DetieneColaYPideSesion() = runTest {
        val f=EscenarioSync.crear(StandardTestDispatcher(testScheduler))
        try {
            val primero=f.cola.guardar(Producto(nombre="Uno",precio=2.0,stock=1)).producto.id
            val segundo=f.cola.guardar(Producto(nombre="Dos",precio=2.0,stock=1)).producto.id
            f.remoto.respuesta={ ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.SESION,"HTTP_401","Sesión vencida")) }
            assertTrue(f.enviar().detenerPorSesion)
            assertEquals(EstadoEnvio.REQUIERE_SESION,f.cola.porId(primero)!!.estado)
            assertEquals(0,f.cola.porId(segundo)!!.intentos)
            assertIs<AccesoSesion.Reautenticar>(f.auth.acceso.value)
        } finally { f.cerrar() }
    }
}
