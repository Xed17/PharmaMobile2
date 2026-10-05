package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class ObtenerProductoUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(id: Long): Result<Producto> = runCatching {
        repository.obtener(id)
    }
}
