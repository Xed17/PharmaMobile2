package pe.edu.upeu.pharmamobile2.domain.usecase

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository

class ObservarUltimaSincronizacionUseCase(
    private val repository: ProductoRepository
) {
    operator fun invoke(): Flow<Long?> = repository.observarUltimaSincronizacion()
}
