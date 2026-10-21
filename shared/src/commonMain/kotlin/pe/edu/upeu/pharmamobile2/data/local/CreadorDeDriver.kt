package pe.edu.upeu.pharmamobile2.data.local

import app.cash.sqldelight.db.SqlDriver

interface CreadorDeDriver {
    fun crear(): SqlDriver
}
