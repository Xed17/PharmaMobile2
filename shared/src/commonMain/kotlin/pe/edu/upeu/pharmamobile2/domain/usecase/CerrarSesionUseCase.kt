package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository

class CerrarSesionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() {
        authRepository.cerrarSesion()
    }
}
