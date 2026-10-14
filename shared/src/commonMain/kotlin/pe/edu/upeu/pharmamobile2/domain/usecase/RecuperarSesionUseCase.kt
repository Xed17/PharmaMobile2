package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.model.Sesion
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository

class RecuperarSesionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Sesion?> {
        return authRepository.recuperarSesion()
    }
}
