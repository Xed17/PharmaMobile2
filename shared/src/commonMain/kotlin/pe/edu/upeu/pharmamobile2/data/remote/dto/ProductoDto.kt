package pe.edu.upeu.pharmamobile2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaginaProductosDto(
    val contenido: List<ProductoDto> = emptyList(),
    val pagina: Int = 0,
    val tamanio: Int = 0,
    val totalElementos: Long = 0,
    val totalPaginas: Int = 0,
    val ultima: Boolean = true
)

@Serializable
data class ProductoDto(
    val id: Int,
    val nombre: String,
    val precio: Double,
    val stock: Int = 0,
    val estado: Boolean = true,
    val categoriaId: Int? = null,
    val categoriaNombre: String? = null,
    val fechaCreacion: String? = null,
    val fechaModificacion: String? = null
)
