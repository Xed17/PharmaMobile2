package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.model.Credenciales
import pe.edu.upeu.pharmamobile2.domain.model.Sesion
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository

class IniciarSesionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(credenciales: Credenciales): Result<Sesion> {
        return authRepository.iniciarSesion(credenciales)
    }
}
