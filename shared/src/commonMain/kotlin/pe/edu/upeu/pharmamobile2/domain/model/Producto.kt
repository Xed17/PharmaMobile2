package pe.edu.upeu.pharmamobile2.domain.model

const val STOCK_MINIMO = 10

data class Producto (
    val id: Int = 0,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean = true
) {
    fun requiereReposicion(): Boolean = stock <= STOCK_MINIMO
    fun verificarStock(cantidad: Int): Boolean {
        return stock >= cantidad
    }
    fun estadoDisponible(): Boolean {
        return stock > 0
    }
    fun esBajoStock(): Boolean {
        return activo && stock <= 5
    }
    fun valorInventario(): Double {
        return precio*stock
    }
    fun disminuirStock(cantidad: Int): Producto {
        require (cantidad > 0) {
            "La cantidad debe ser mayor que 0"
        }
        require (verificarStock(cantidad)) {
            "Stock insuficiente"
        }
        return copy (
            stock = stock - cantidad
        )
    }
}