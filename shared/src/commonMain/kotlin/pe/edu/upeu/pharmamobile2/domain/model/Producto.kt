package pe.edu.upeu.pharmamobile2.domain.model

const val STOCK_MINIMO = 10

data class Producto(
    val id: Long = 0L,
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val activo: Boolean = true,
    val categoriaId: Long = 26L,
    val categoriaNombre: String? = "Analgésicos"
) {
    // Constructor de conveniencia para compatibilidad con Int literals (ej: id = 1)
    constructor(
        id: Int,
        nombre: String,
        precio: Double,
        stock: Int,
        activo: Boolean = true,
        categoriaId: Long = 26L,
        categoriaNombre: String? = "Analgésicos"
    ) : this(
        id = id.toLong(),
        nombre = nombre,
        precio = precio,
        stock = stock,
        activo = activo,
        categoriaId = categoriaId,
        categoriaNombre = categoriaNombre
    )

    fun requiereReposicion(): Boolean = stock <= STOCK_MINIMO
    fun verificarStock(cantidad: Int): Boolean = stock >= cantidad
    fun estadoDisponible(): Boolean = stock > 0
    fun esBajoStock(): Boolean = activo && stock <= 5
    fun valorInventario(): Double = precio * stock
    fun disminuirStock(cantidad: Int): Producto {
        require(cantidad > 0) { "La cantidad debe ser mayor que 0" }
        require(verificarStock(cantidad)) { "Stock insuficiente" }
        return copy(stock = stock - cantidad)
    }
}