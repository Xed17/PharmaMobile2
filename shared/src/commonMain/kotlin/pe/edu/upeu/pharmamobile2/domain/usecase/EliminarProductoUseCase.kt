package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class EliminarProductoUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        repository.eliminar(id)
    }
}
