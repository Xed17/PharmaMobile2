package pe.edu.upeu.pharmamobile2.presentation.producto

import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.platform.formatearSoles

data class ProductoUi(
    val id: Long,
    val nombre: String,
    val precio: String,
    val stock: Int,
    val activo: Boolean = true,
    val categoriaId: Long = 26L,
    val categoriaNombre: String? = null,
    val precioNumerico: Double = 0.0
) {
    fun esBajoStock(): Boolean = stock <= 10
}

fun Producto.toUi(): ProductoUi {
    return ProductoUi(
        id = id,
        nombre = nombre,
        precio = formatearSoles(precio),
        stock = stock,
        activo = activo,
        categoriaId = categoriaId,
        categoriaNombre = categoriaNombre,
        precioNumerico = precio
    )
}

fun ProductoUi.toDomain(): Producto {
    return Producto(
        id = id,
        nombre = nombre,
        precio = precioNumerico,
        stock = stock,
        activo = activo,
        categoriaId = categoriaId,
        categoriaNombre = categoriaNombre
    )
}
