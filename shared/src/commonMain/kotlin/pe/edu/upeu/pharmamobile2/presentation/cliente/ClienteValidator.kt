package pe.edu.upeu.pharmamobile2.presentation.cliente

data class ResultadoValidacionCliente(
    val esValido: Boolean,
    val errorNombre: String? = null,
    val errorCorreo: String? = null,
    val errorTelefono: String? = null,
    val mensajeGeneral: String? = null
)

object ClienteValidator {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val phoneRegex = Regex("^[0-9]{7,15}$")

    fun validarNombre(nombre: String): String? {
        return if (nombre.isBlank()) {
            "El nombre es obligatorio."
        } else {
            null
        }
    }

    fun validarCorreo(correo: String): String? {
        return when {
            correo.isBlank() -> "El correo electrónico es obligatorio."
            !emailRegex.matches(correo.trim()) -> "Ingrese un correo electrónico válido."
            else -> null
        }
    }

    fun validarTelefono(telefono: String): String? {
        val trimmed = telefono.trim()
        return when {
            trimmed.isEmpty() -> null // Opcional
            !phoneRegex.matches(trimmed) -> "El teléfono debe contener solo dígitos (7 a 15 números)."
            else -> null
        }
    }

    fun validar(nombre: String, correo: String, telefono: String): ResultadoValidacionCliente {
        val errorNombre = validarNombre(nombre)
        if (errorNombre != null) {
            return ResultadoValidacionCliente(
                esValido = false,
                errorNombre = errorNombre,
                mensajeGeneral = errorNombre
            )
        }

        val errorCorreo = validarCorreo(correo)
        if (errorCorreo != null) {
            return ResultadoValidacionCliente(
                esValido = false,
                errorCorreo = errorCorreo,
                mensajeGeneral = errorCorreo
            )
        }

        val errorTelefono = validarTelefono(telefono)
        if (errorTelefono != null) {
            return ResultadoValidacionCliente(
                esValido = false,
                errorTelefono = errorTelefono,
                mensajeGeneral = errorTelefono
            )
        }

        return ResultadoValidacionCliente(esValido = true)
    }
}
