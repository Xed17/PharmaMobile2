package pe.edu.upeu.pharmamobile2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductoRequestDto(
    val nombre: String,
    val precio: Double,
    val stock: Int,
    val estado: Boolean = true,
    val categoriaId: Long
)
