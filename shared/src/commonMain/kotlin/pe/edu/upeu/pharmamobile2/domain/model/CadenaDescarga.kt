package pe.edu.upeu.pharmamobile2.domain.model

enum class EstadoCadena { NUEVA, EN_CURSO, ESPERA, AGOTADA, BLOQUEADA, COMPLETA }
data class CadenaDescarga(val id: String, val estado: EstadoCadena, val intentos: Int, val proximoEnMs: Long?, val ultimoFallo: FalloOperacion?)
