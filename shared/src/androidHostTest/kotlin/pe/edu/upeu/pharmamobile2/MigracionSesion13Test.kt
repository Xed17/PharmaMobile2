package pe.edu.upeu.pharmamobile2

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.flow.first
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import kotlin.test.*

class MigracionSesion13Test {
    @Test fun conservaLegadoSinAdjudicarloAlSiguienteUsuario() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(null, "CREATE TABLE producto(id INTEGER NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, precio REAL NOT NULL, stock INTEGER NOT NULL, pendiente INTEGER NOT NULL DEFAULT 0)", 0)
            driver.execute(null, "CREATE TABLE metadato(clave TEXT NOT NULL PRIMARY KEY, valor TEXT NOT NULL)", 0)
            driver.execute(null, "INSERT INTO producto VALUES(-1, 'Producto previo', 7.5, 13, 1)", 0)
            driver.execute(null, "INSERT INTO producto VALUES(2, 'Confirmado previo', 9.0, 5, 0)", 0)
            driver.execute(null, "INSERT INTO metadato VALUES('ultima_sincronizacion', '1000')", 0)
            PharmaDb.Schema.migrate(driver, 1, PharmaDb.Schema.version)
            val local = ProductoLocal(PharmaDb(driver), ContextoCuenta("A", 1), StandardTestDispatcher(testScheduler))
            assertTrue(local.observar().first().isEmpty())
            assertNull(local.observarUltimaSincronizacion().first())
            val legados = driver.executeQuery(null, "SELECT COUNT(*) FROM producto WHERE cuentaClave='legacy_unassigned'", { c -> c.next(); QueryResult.Value(c.getLong(0)!!) }, 0).value
            assertEquals(2L, legados)
            val nombre = driver.executeQuery(null, "SELECT nombre FROM producto WHERE cuentaClave='legacy_unassigned' AND id=-1", { c -> c.next(); QueryResult.Value(c.getString(0)) }, 0).value
            assertEquals("Producto previo", nombre)
        } finally { driver.close() }
    }
}
