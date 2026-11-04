package pe.edu.upeu.pharmamobile2.domain.model

enum class NaturalezaFallo { TRANSITORIO, PERMANENTE, SESION, PERMISOS }
data class FalloOperacion(val naturaleza: NaturalezaFallo, val codigo: String, val mensajeSeguro: String, val campos: Map<String, String> = emptyMap(), val efectoIncierto: Boolean = false)
