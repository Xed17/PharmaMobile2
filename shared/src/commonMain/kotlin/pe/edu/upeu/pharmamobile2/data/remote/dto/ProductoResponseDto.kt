package pe.edu.upeu.pharmamobile2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductoResponseDto(
    val id: Long,
    val nombre: String,
    val precio: Double,
    val stock: Int = 0,
    val estado: Boolean = true,
    val categoriaId: Long? = null,
    val categoriaNombre: String? = null,
    val fechaCreacion: String? = null,
    val fechaModificacion: String? = null
)
