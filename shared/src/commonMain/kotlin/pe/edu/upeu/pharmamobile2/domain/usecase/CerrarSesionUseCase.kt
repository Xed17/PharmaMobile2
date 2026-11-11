package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.port.LimpiadorDatosSesion
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository

class CerrarSesionUseCase(
    private val authRepository: AuthRepository,
    private val limpiador: LimpiadorDatosSesion
) {
    suspend operator fun invoke() {
        authRepository.cerrarSesion()
        limpiador.limpiar()
    }
}
