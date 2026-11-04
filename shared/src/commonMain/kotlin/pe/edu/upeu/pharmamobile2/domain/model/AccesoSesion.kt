package pe.edu.upeu.pharmamobile2.domain.model

enum class ModoAcceso { REMOTO, LOCAL }
sealed interface AccesoSesion {
    data object SinSesion : AccesoSesion
    data class Disponible(val contexto: ContextoCuenta, val perfil: PerfilLocal, val modo: ModoAcceso) : AccesoSesion
    data class Reautenticar(val contexto: ContextoCuenta, val perfil: PerfilLocal, val motivo: String) : AccesoSesion
}
