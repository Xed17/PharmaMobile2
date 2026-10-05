package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class ListarProductosUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(): Result<List<Producto>> = runCatching {
        repository.listar()
    }
}
