package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.testutil.crearDriverPruebas
import kotlin.test.*

class ProductoLocalCuentaTest {
    @Test fun aislaFilasYMetadatosInclusoConMismosIds() = runTest {
        val driver = crearDriverPruebas()
        try {
            val db = PharmaDb(driver)
            val a = ProductoLocal(db, ContextoCuenta("A", 1), StandardTestDispatcher(testScheduler))
            val b = ProductoLocal(db, ContextoCuenta("B", 1), StandardTestDispatcher(testScheduler))
            val pa = a.guardarPendiente(Producto(nombre="Privado A", precio=3.5, stock=2, activo=false, categoriaId=7, categoriaNombre="Otra"))
            val pb = b.guardarPendiente(Producto(nombre="Privado B", precio=4.5, stock=3))
            assertEquals(-1L, pa.id)
            assertEquals(-1L, pb.id)
            assertEquals(listOf("Privado A"), a.observar().first().map { it.nombre })
            assertEquals(listOf("Privado B"), b.observar().first().map { it.nombre })
            assertFalse(a.observar().first().single().activo)
            assertEquals(7L, a.observar().first().single().categoriaId)
            a.guardarUltimaSincronizacion(1000)
            assertNull(b.observarUltimaSincronizacion().first())
            a.eliminarLocal(-1)
            assertEquals(1, b.observar().first().size)
        } finally { driver.close() }
    }
}
