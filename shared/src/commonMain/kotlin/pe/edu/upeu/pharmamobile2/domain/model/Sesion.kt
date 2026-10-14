package pe.edu.upeu.pharmamobile2.domain.model

data class Sesion(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long = 3600L,
    val username: String,
    val nombreCompleto: String,
    val roles: List<String> = emptyList(),
    val permisos: Set<String> = emptySet()
) {
    fun puede(permiso: String): Boolean = permisos.contains(permiso)
}
