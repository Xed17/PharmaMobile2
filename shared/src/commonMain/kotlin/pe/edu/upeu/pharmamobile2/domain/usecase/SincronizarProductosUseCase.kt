package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.port.SincronizadorProductos

class SincronizarProductosUseCase(private val sincronizador: SincronizadorProductos) {
    suspend operator fun invoke() = sincronizador.sincronizar()
}
