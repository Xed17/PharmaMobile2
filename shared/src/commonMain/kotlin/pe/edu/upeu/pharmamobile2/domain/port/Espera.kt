package pe.edu.upeu.pharmamobile2.domain.port

interface Espera { suspend fun esperar(ms: Long) }
