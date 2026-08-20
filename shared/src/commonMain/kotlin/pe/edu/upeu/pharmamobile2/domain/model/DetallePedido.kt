package pe.edu.upeu.pharmamobile2.domain.model

data class DetallePedido (
    val producto: Producto,
    val cantidad: Int,
) {
    init {
        require (cantidad > 0) {
            "La cantidad debe ser mayor que 0"
        }
    }
    fun subTotal(): Double {
        return producto.precio*cantidad
    }
}