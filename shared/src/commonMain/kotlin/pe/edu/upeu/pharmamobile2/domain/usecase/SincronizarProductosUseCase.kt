package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class SincronizarProductosUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke() {
        repository.sincronizar()
    }
}
