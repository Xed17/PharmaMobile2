package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.test.*
import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.testutil.*
import kotlin.test.*

class BorradorProductoTest {
    @Test fun conservaTextoIncompletoYAislaSuCuenta() = runTest {
        val driver=crearDriverPruebas()
        try {
            val db=PharmaDb(driver);val d=StandardTestDispatcher(testScheduler)
            val a=BorradorProductoLocal(db,ContextoCuenta("A",1),d)
            val b=BorradorProductoLocal(db,ContextoCuenta("B",1),d)
            a.guardar(BorradorProducto(null,"Sin terminar","12.","",7,"Otra"))
            assertEquals("12.",a.observar().first()!!.precioTexto)
            assertNull(b.observar().first())
            a.borrar()
            assertNull(a.observar().first())
        } finally { driver.close() }
    }
    @Test fun cadenaPersistidaYConciliacionNoBorranPendientes() = runTest {
        val driver=crearDriverPruebas()
        try {
            val db=PharmaDb(driver);val a=ContextoCuenta("A",1);val b=ContextoCuenta("B",1)
            val descarga=DescargaLocal(db,a)
            descarga.guardar(CadenaDescarga("cadena-1",EstadoCadena.ESPERA,2,3000,null))
            assertEquals(2,DescargaLocal(db,a).actual()!!.intentos)
            assertNull(DescargaLocal(db,b).actual())
            val local=ProductoLocal(db,a,StandardTestDispatcher(testScheduler))
            local.guardarPendiente(Producto(nombre="Pendiente",precio=2.0,stock=3))
            descarga.conciliarCompleta(listOf(Producto(id=30L,nombre="Remoto",precio=4.0,stock=8)),5000)
            assertEquals(setOf(-1L,30L),local.listar().map { it.id }.toSet())
            assertEquals(5000L,descarga.observarUltimaSync().first())
            assertNull(DescargaLocal(db,b).observarUltimaSync().first())
        } finally { driver.close() }
    }
}
