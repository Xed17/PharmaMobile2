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
    val precioNumerico: Double = 0.0,
    val esPendiente: Boolean = id < 0,
    val estadoEnvioTexto: String? = null,
    val motivoEstado: String? = null
) {
    fun esBajoStock(): Boolean = stock <= 10
}

fun Producto.toUi(estadoTexto: String? = null, motivo: String? = null): ProductoUi {
    return ProductoUi(
        id = id,
        nombre = nombre,
        precio = formatearSoles(precio),
        stock = stock,
        activo = activo,
        categoriaId = categoriaId,
        categoriaNombre = categoriaNombre,
        precioNumerico = precio,
        esPendiente = id < 0,
        estadoEnvioTexto = estadoTexto ?: if (id < 0) "Pendiente" else null,
        motivoEstado = motivo
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
