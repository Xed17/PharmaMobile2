package pe.edu.upeu.pharmamobile2.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val registrarProducto: RegistrarProductoUseCase,
    private val repository: ProductoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
    }

    fun actualizarNombre(valor: String) {
        _uiState.update { it.copy(nombre = valor, errorNombre = null, mensajeFormulario = null) }
    }

    fun actualizarPrecio(valor: String) {
        _uiState.update { it.copy(precio = valor, errorPrecio = null, mensajeFormulario = null) }
    }

    fun actualizarStock(valor: String) {
        _uiState.update { it.copy(stock = valor, errorStock = null, mensajeFormulario = null) }
    }

    fun cargarProductos() {
        viewModelScope.launch {
            _uiState.update { it.copy(fase = FaseProductos.Cargando) }
            runCatching { repository.listar() }
                .onSuccess { productos ->
                    _uiState.update {
                        it.copy(
                            fase = if (productos.isEmpty()) {
                                FaseProductos.SinProductos
                            } else {
                                FaseProductos.ConProductos(productos)
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(fase = FaseProductos.Error(error.message ?: "Error inesperado"))
                    }
                }
        }
    }

    fun registrar() {
        viewModelScope.launch {
            val actual = _uiState.value
            val precioNumero = actual.precio.toDoubleOrNull() ?: -1.0
            val stockNumero = actual.stock.toIntOrNull() ?: -1

            val producto = Producto(
                nombre = actual.nombre.trim(),
                precio = precioNumero,
                stock = stockNumero,
                activo = true
            )

            registrarProducto(producto)
                .onSuccess {
                    limpiarFormulario()
                    _uiState.update {
                        it.copy(
                            mensajeFormulario = "Producto registrado correctamente",
                            esErrorFormulario = false
                        )
                    }
                    cargarProductos()
                }
                .onFailure { error ->
                    val errorNombre = if (actual.nombre.isBlank()) "El nombre es obligatorio" else null
                    val errorPrecio = if (actual.precio.toDoubleOrNull() == null) {
                        "Ingrese un precio numérico"
                    } else if (precioNumero <= 0.0) {
                        "El precio debe ser mayor que cero"
                    } else null

                    val errorStock = if (actual.stock.toIntOrNull() == null) {
                        "Ingrese un stock entero"
                    } else if (stockNumero < 0) {
                        "El stock no puede ser negativo"
                    } else null

                    _uiState.update {
                        it.copy(
                            errorNombre = errorNombre,
                            errorPrecio = errorPrecio,
                            errorStock = errorStock,
                            mensajeFormulario = error.message ?: "Error en los datos ingresados",
                            esErrorFormulario = true
                        )
                    }
                }
        }
    }

    private fun limpiarFormulario() {
        _uiState.update {
            it.copy(
                nombre = "",
                precio = "",
                stock = "",
                errorNombre = null,
                errorPrecio = null,
                errorStock = null,
                mensajeFormulario = null,
                esErrorFormulario = false
            )
        }
    }
}
