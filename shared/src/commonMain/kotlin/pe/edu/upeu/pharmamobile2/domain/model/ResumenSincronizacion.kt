package pe.edu.upeu.pharmamobile2.domain.model

data class ResumenSincronizacion(val enviadas: Int, val pendientes: Int, val fallidas: Int, val revision: Int, val bloqueadas: Int, val descargaCompleta: Boolean, val ultimaSync: Long?)
