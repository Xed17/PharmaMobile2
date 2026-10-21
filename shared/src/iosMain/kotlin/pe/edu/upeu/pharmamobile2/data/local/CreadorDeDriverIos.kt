package pe.edu.upeu.pharmamobile2.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

class CreadorDeDriverIos : CreadorDeDriver {
    override fun crear(): SqlDriver {
        return NativeSqliteDriver(
            schema = PharmaDb.Schema,
            name = "pharma.db"
        )
    }
}
