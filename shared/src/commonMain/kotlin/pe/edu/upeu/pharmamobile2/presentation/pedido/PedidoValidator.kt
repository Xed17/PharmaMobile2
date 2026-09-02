package pe.edu.upeu.pharmamobile2.presentation.pedido

import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import pe.edu.upeu.pharmamobile2.domain.model.Producto

data class ResultadoValidacionPedido(
    val esValido: Boolean,
    val errorCliente: String? = null,
    val errorProducto: String? = null,
    val errorCantidad: String? = null,
    val mensajeGeneral: String? = null
)

object PedidoValidator {

    fun validarCliente(cliente: Cliente?): String? {
        return if (cliente == null) {
            "Debe seleccionar un cliente."
        } else {
            null
        }
    }

    fun validarProducto(producto: Producto?): String? {
        return if (producto == null) {
            "Debe seleccionar un producto."
        } else {
            null
        }
    }

    fun validarCantidad(cantidad: String, producto: Producto?): String? {
        val cantidadNumero = cantidad.toIntOrNull()
        return when {
            cantidad.isBlank() -> "La cantidad es obligatoria."
            cantidadNumero == null -> "Ingrese una cantidad entera."
            cantidadNumero <= 0 -> "La cantidad debe ser mayor que cero."
            producto != null && !producto.verificarStock(cantidadNumero) ->
                "Stock insuficiente (Disponible: ${producto.stock})."
            else -> null
        }
    }

    fun validar(
        cliente: Cliente?,
        producto: Producto?,
        cantidad: String
    ): ResultadoValidacionPedido {
        val errorCliente = validarCliente(cliente)
        if (errorCliente != null) {
            return ResultadoValidacionPedido(
                esValido = false,
                errorCliente = errorCliente,
                mensajeGeneral = errorCliente
            )
        }

        val errorProducto = validarProducto(producto)
        if (errorProducto != null) {
            return ResultadoValidacionPedido(
                esValido = false,
                errorProducto = errorProducto,
                mensajeGeneral = errorProducto
            )
        }

        val errorCantidad = validarCantidad(cantidad, producto)
        if (errorCantidad != null) {
            return ResultadoValidacionPedido(
                esValido = false,
                errorCantidad = errorCantidad,
                mensajeGeneral = errorCantidad
            )
        }

        return ResultadoValidacionPedido(esValido = true)
    }
}
