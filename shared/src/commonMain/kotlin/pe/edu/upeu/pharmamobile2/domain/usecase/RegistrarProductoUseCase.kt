package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import kotlinx.coroutines.CancellationException

class RegistrarProductoUseCase(
    private val repository: ProductoRepository
) {
    suspend operator fun invoke(producto: Producto): Result<Producto> = try {
        require(producto.nombre.isNotBlank()) {
            "El nombre es obligatorio"
        }
        require(producto.precio.isFinite() && producto.precio > 0) {
            "El precio debe ser mayor que cero"
        }
        require(producto.stock >= 0) {
            "El stock no puede ser negativo"
        }

        Result.success(repository.registrar(producto))
    } catch (cancelacion: CancellationException) {
        throw cancelacion
    } catch (e: Exception) {
        Result.failure(e)
    }
}
