package pe.edu.upeu.pharmamobile2.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsuarioActualDto(
    val id: Long? = null,
    val username: String,
    val empleadoId: Long? = null,
    val empleadoNombre: String? = null,
    val cargo: String? = null,
    val roles: List<String> = emptyList(),
    val permisos: List<String> = emptyList(),
    val estado: Boolean = true,
    val fechaCreacion: String? = null
)
