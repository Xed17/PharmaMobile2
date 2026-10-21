package pe.edu.upeu.pharmamobile2.testutil

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

actual fun crearDriverPruebas(): SqlDriver {
    return inMemoryDriver(PharmaDb.Schema)
}
