package pe.edu.upeu.pharmamobile2.presentation.producto

data class ResultadoValidacionProducto(
    val esValido: Boolean,
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorStock: String? = null,
    val mensajeGeneral: String? = null
)

object ProductoValidator {

    fun validarNombre(nombre: String): String? {
        return if (nombre.isBlank()) {
            "Ingrese nombre del producto"
        } else {
            null
        }
    }

    fun validarPrecio(precio: String): String? {
        val precioNumero = precio.toDoubleOrNull()
        return if (precio.isBlank() || precioNumero == null || precioNumero <= 0.0) {
            "Ingrese precio válido"
        } else {
            null
        }
    }

    fun validarStock(stock: String): String? {
        val stockNumero = stock.toIntOrNull()
        return when {
            stock.isBlank() -> "Ingrese el stock"
            stockNumero == null -> "Ingrese un stock entero válido"
            stockNumero < 0 -> "El stock no puede ser negativo"
            else -> null
        }
    }

    fun validar(nombre: String, precio: String, stock: String): ResultadoValidacionProducto {
        val errorNombre = validarNombre(nombre)
        val errorPrecio = validarPrecio(precio)
        val errorStock = validarStock(stock)

        val esValido = errorNombre == null && errorPrecio == null && errorStock == null
        val primerError = errorNombre ?: errorPrecio ?: errorStock

        return ResultadoValidacionProducto(
            esValido = esValido,
            errorNombre = errorNombre,
            errorPrecio = errorPrecio,
            errorStock = errorStock,
            mensajeGeneral = primerError
        )
    }
}
