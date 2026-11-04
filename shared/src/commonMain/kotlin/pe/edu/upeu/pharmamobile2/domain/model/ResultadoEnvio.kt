package pe.edu.upeu.pharmamobile2.domain.model

sealed interface ResultadoEnvio {
    data class Exito(val producto: Producto) : ResultadoEnvio
    data class Fallo(val error: FalloOperacion) : ResultadoEnvio
}
