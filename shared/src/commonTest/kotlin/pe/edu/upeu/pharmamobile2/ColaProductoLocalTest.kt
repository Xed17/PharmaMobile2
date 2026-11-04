package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.ColaProductoLocal
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.testutil.*
import kotlin.test.*

class ColaProductoLocalTest {
    @Test fun inicioAtomicoEInterrupcionNoDevuelveFilaAPendiente() = runTest {
        val driver = crearDriverPruebas()
        try {
            val db = PharmaDb(driver)
            val reloj = RelojFalso(1000)
            val cola = ColaProductoLocal(db, ContextoCuenta("A",1), reloj, GeneradorIdsSecuencial(), StandardTestDispatcher(testScheduler))
            val fila = cola.guardar(Producto(nombre="Prueba",precio=3.5,stock=2))
            assertNotNull(cola.iniciar(fila.producto.id, 1000))
            assertNull(cola.iniciar(fila.producto.id, 1000))
            assertEquals(1, cola.porId(fila.producto.id)!!.intentos)
            cola.recuperarInterrumpidos()
            assertEquals(EstadoEnvio.REVISION,cola.porId(fila.producto.id)!!.estado)
            assertTrue(cola.elegibles().isEmpty())
        } finally { driver.close() }
    }
    @Test fun cincoIntentosInciertosAgotanSinBorrarLaFila() = runTest {
        val driver = crearDriverPruebas()
        try {
            val reloj=RelojFalso(1000)
            val cola=ColaProductoLocal(PharmaDb(driver),ContextoCuenta("A",1),reloj,GeneradorIdsSecuencial(),StandardTestDispatcher(testScheduler))
            val id=cola.guardar(Producto(nombre="Prueba",precio=3.5,stock=2)).producto.id
            repeat(5) { n ->
                assertNotNull(cola.iniciar(id,reloj.ahoraMs()))
                cola.resolver(id,ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.TRANSITORIO,"TIMEOUT","Tiempo agotado",efectoIncierto=true)),reloj.ahoraMs())
                reloj.instanteMs += 16000
                if(n<4) cola.autorizarReenvio(id,reloj.ahoraMs(),true)
            }
            assertEquals(EstadoEnvio.FALLIDA_AGOTADA,cola.porId(id)!!.estado)
            assertEquals(5,cola.porId(id)!!.intentos)
            assertTrue(cola.elegibles().isEmpty())
            assertNull(cola.iniciar(id,reloj.ahoraMs()))
        } finally { driver.close() }
    }
    @Test fun rechazoPermanentePersisteCamposYNoBloqueaOtraCuenta() = runTest {
        val driver=crearDriverPruebas()
        try {
            val db=PharmaDb(driver);val reloj=RelojFalso(1000);val ids=GeneradorIdsSecuencial()
            val a=ColaProductoLocal(db,ContextoCuenta("A",1),reloj,ids,StandardTestDispatcher(testScheduler))
            val b=ColaProductoLocal(db,ContextoCuenta("B",1),reloj,ids,StandardTestDispatcher(testScheduler))
            val id=a.guardar(Producto(nombre="Prueba",precio=3.5,stock=2)).producto.id
            a.iniciar(id,1000)
            a.resolver(id,ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.PERMANENTE,"400","Revisar campos",mapOf("nombre" to "Nombre inválido"))),1000)
            assertEquals(EstadoEnvio.FALLIDA_PERMANENTE,a.porId(id)!!.estado)
            assertEquals("Nombre inválido",a.porId(id)!!.ultimoError!!.campos["nombre"])
            assertTrue(a.elegibles().isEmpty())
            assertTrue(b.observar().first().isEmpty())
        } finally { driver.close() }
    }
}
