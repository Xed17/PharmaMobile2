package pe.edu.upeu.pharmamobile2.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pe.edu.upeu.pharmamobil.db.PharmaDb

class CreadorDeDriverAndroid(
    private val context: Context
) : CreadorDeDriver {
    override fun crear(): SqlDriver {
        return AndroidSqliteDriver(
            schema = PharmaDb.Schema,
            context = context,
            name = "pharma.db"
        )
    }
}
