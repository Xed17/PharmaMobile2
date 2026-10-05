package pe.edu.upeu.pharmamobile2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponseDto(
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
    val path: String? = null,
    val validationErrors: Map<String, String>? = null
)
