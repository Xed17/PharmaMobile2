package pe.edu.upeu.pharmamobile2.domain.model

data class RegistroEnvio(val producto: Producto, val estado: EstadoEnvio, val operacionLocalId: String, val revision: Int, val intentos: Int, val ultimoError: FalloOperacion?, val proximoIntentoEnMs: Long?)
