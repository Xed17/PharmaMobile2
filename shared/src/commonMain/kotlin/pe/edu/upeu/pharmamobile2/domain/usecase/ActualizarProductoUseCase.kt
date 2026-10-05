package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class ActualizarProductoUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(producto: Producto): Result<Producto> = runCatching {
        require(producto.id > 0L) {
            "El identificador del producto es inválido"
        }
        require(producto.nombre.isNotBlank()) {
            "El nombre es obligatorio"
        }
        require(producto.precio > 0) {
            "El precio debe ser mayor que cero"
        }
        require(producto.stock >= 0) {
            "El stock no puede ser negativo"
        }

        repository.actualizar(producto)
    }
}
