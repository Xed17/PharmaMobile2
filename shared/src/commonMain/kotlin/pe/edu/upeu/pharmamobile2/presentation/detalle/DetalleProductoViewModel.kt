package pe.edu.upeu.pharmamobile2.presentation.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.comoTextoParaCompartir
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoUi
import pe.edu.upeu.pharmamobile2.presentation.producto.toDomain
import pe.edu.upeu.pharmamobile2.presentation.producto.toUi

data class DetalleUiState(
    val cargando: Boolean = false,
    val producto: ProductoUi? = null,
    val error: String? = null
)

class DetalleProductoViewModel(
    private val repository: ProductoRepository,
    private val compartidor: Compartidor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalleUiState())
    val uiState: StateFlow<DetalleUiState> = _uiState.asStateFlow()

    private var productoOriginal: Producto? = null

    fun cargar(id: Long) = viewModelScope.launch {
        _uiState.value = DetalleUiState(cargando = true)
        runCatching { repository.obtener(id) }
            .onSuccess { prod ->
                productoOriginal = prod
                _uiState.value = DetalleUiState(cargando = false, producto = prod.toUi())
            }
            .onFailure { fallo ->
                _uiState.value = DetalleUiState(
                    cargando = false,
                    error = fallo.message ?: "Error al obtener producto"
                )
            }
    }

    fun establecerProducto(prod: Producto) {
        productoOriginal = prod
        _uiState.value = DetalleUiState(cargando = false, producto = prod.toUi())
    }

    fun compartir(producto: Producto? = null) {
        val prod = producto ?: productoOriginal ?: _uiState.value.producto?.toDomain() ?: return
        compartidor.compartir(
            prod.comoTextoParaCompartir()
        )
    }
}
