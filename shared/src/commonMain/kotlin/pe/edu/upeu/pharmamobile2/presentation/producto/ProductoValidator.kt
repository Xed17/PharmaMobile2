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
        return if (!nombre.isNotBlank()) {
            "El nombre es obligatorio."
        } else {
            null
        }
    }

    fun validarPrecio(precio: String): String? {
        val precioNumero = precio.toDoubleOrNull()
        return when {
            precioNumero == null -> "Ingrese un precio numérico."
            precioNumero <= 0.0 -> "El precio debe ser mayor que cero."
            else -> null
        }
    }

    fun validarStock(stock: String): String? {
        val stockNumero = stock.toIntOrNull()
        return when {
            stockNumero == null -> "Ingrese un stock entero."
            stockNumero < 0 -> "El stock no puede ser negativo."
            else -> null
        }
    }

    fun validar(nombre: String, precio: String, stock: String): ResultadoValidacionProducto {
        val precioNumero = precio.toDoubleOrNull()
        val stockNumero = stock.toIntOrNull()

        return when {
            !nombre.isNotBlank() -> ResultadoValidacionProducto(
                esValido = false,
                errorNombre = "El nombre es obligatorio.",
                mensajeGeneral = "El nombre es obligatorio."
            )
            precioNumero == null -> ResultadoValidacionProducto(
                esValido = false,
                errorPrecio = "Ingrese un precio numérico.",
                mensajeGeneral = "Ingrese un precio numérico."
            )
            precioNumero <= 0.0 -> ResultadoValidacionProducto(
                esValido = false,
                errorPrecio = "El precio debe ser mayor que cero.",
                mensajeGeneral = "El precio debe ser mayor que cero."
            )
            stockNumero == null -> ResultadoValidacionProducto(
                esValido = false,
                errorStock = "Ingrese un stock entero.",
                mensajeGeneral = "Ingrese un stock entero."
            )
            stockNumero < 0 -> ResultadoValidacionProducto(
                esValido = false,
                errorStock = "El stock no puede ser negativo.",
                mensajeGeneral = "El stock no puede ser negativo."
            )
            else -> ResultadoValidacionProducto(esValido = true)
        }
    }
}
