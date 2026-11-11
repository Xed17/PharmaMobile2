package pe.edu.upeu.pharmamobile2.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PerfilLocal(val username: String, val nombreCompleto: String, val roles: List<String>, val permisos: Set<String>, val validadoEnMs: Long, val expiraTokenEnMs: Long?)
