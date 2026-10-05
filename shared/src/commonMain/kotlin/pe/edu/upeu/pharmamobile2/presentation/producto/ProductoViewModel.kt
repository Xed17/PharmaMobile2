package pe.edu.upeu.pharmamobile2.presentation.producto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApi
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase

class ProductoViewModel(
    private val listarProductos: ListarProductosUseCase,
    private val obtenerProducto: ObtenerProductoUseCase,
    private val registrarProducto: RegistrarProductoUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase
) : ViewModel() {

    // Constructor de conveniencia para tests o inicialización directa con repositorio
    constructor(
        registrarUseCase: RegistrarProductoUseCase,
        repository: ProductoRepository
    ) : this(
        listarProductos = ListarProductosUseCase(repository),
        obtenerProducto = ObtenerProductoUseCase(repository),
        registrarProducto = registrarUseCase,
        actualizarProducto = ActualizarProductoUseCase(repository),
        eliminarProducto = EliminarProductoUseCase(repository)
    )

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        cargarProductos()
    }

    fun cargarProductos() {
        viewModelScope.launch {
            // Si la pantalla ya tiene productos visibles, conservamos la lista y no mostramos pantalla completa de carga
            if (_uiState.value.fase !is ProductoUiState.Fase.ConProductos) {
                _uiState.update { it.copy(fase = ProductoUiState.Fase.Cargando) }
            }

            listarProductos()
                .onSuccess { productos ->
                    _uiState.update {
                        it.copy(
                            fase = if (productos.isEmpty()) {
                                ProductoUiState.Fase.SinProductos
                            } else {
                                ProductoUiState.Fase.ConProductos(productos)
                            }
                        )
                    }
                }
                .onFailure { fallo ->
                    val mensaje = mensajeDe(fallo)
                    _uiState.update {
                        // Si ya habían productos, no destruimos la lista, informamos operación fallida
                        if (it.fase is ProductoUiState.Fase.ConProductos) {
                            it.copy(operacion = ProductoUiState.Operacion.Fallida(mensaje))
                        } else {
                            it.copy(fase = ProductoUiState.Fase.Error(mensaje))
                        }
                    }
                }
        }
    }

    fun actualizarNombre(valor: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(nombre = valor, nombreError = null))
        }
    }

    fun actualizarPrecio(valor: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(precio = valor, precioError = null))
        }
    }

    fun actualizarStock(valor: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(stock = valor, stockError = null))
        }
    }

    fun actualizarCategoria(id: Long, nombre: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(categoriaId = id, categoriaNombre = nombre))
        }
    }

    fun seleccionarParaEditar(producto: Producto) {
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(
                    id = producto.id,
                    nombre = producto.nombre,
                    precio = producto.precio.toString(),
                    stock = producto.stock.toString(),
                    categoriaId = producto.categoriaId,
                    categoriaNombre = producto.categoriaNombre ?: "General"
                ),
                mensajeExito = null
            )
        }
    }

    fun cancelarEdicion() {
        limpiarFormulario()
    }

    fun limpiarMensajeExito() {
        _uiState.update { it.copy(mensajeExito = null) }
    }

    fun limpiarOperacion() {
        _uiState.update { it.copy(operacion = ProductoUiState.Operacion.Inactiva) }
    }

    fun guardar() {
        val form = _uiState.value.formulario
        if (form.estaEnModoEdicion) {
            ejecutarActualizacion()
        } else {
            ejecutarCreacion()
        }
    }

    private fun ejecutarCreacion() = viewModelScope.launch {
        val form = _uiState.value.formulario

        val precioNum = form.precio.toDoubleOrNull() ?: 0.0
        val stockNum = form.stock.toIntOrNull() ?: -1

        val nuevoProducto = Producto(
            id = 0L,
            nombre = form.nombre.trim(),
            precio = precioNum,
            stock = stockNum,
            activo = true,
            categoriaId = form.categoriaId,
            categoriaNombre = form.categoriaNombre
        )

        _uiState.update {
            it.copy(operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Crear))
        }

        registrarProducto(nuevoProducto)
            .onSuccess {
                limpiarFormulario()
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Inactiva,
                        mensajeExito = "Producto registrado correctamente"
                    )
                }
                cargarProductos()
            }
            .onFailure { fallo ->
                manejarFallo(fallo)
            }
    }

    private fun ejecutarActualizacion() = viewModelScope.launch {
        val form = _uiState.value.formulario
        val idProducto = form.id ?: return@launch

        val precioNum = form.precio.toDoubleOrNull() ?: 0.0
        val stockNum = form.stock.toIntOrNull() ?: -1

        val productoActualizado = Producto(
            id = idProducto,
            nombre = form.nombre.trim(),
            precio = precioNum,
            stock = stockNum,
            activo = true,
            categoriaId = form.categoriaId,
            categoriaNombre = form.categoriaNombre
        )

        _uiState.update {
            it.copy(operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Actualizar))
        }

        actualizarProducto(productoActualizado)
            .onSuccess {
                limpiarFormulario()
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Inactiva,
                        mensajeExito = "Producto actualizado correctamente"
                    )
                }
                cargarProductos()
            }
            .onFailure { fallo ->
                manejarFallo(fallo)
            }
    }

    fun eliminar(id: Long) = viewModelScope.launch {
        _uiState.update {
            it.copy(operacion = ProductoUiState.Operacion.EnCurso(ProductoUiState.Operacion.Tipo.Eliminar))
        }

        eliminarProducto(id)
            .onSuccess {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Inactiva,
                        mensajeExito = "Producto eliminado correctamente"
                    )
                }
                cargarProductos()
            }
            .onFailure { fallo ->
                manejarFallo(fallo)
            }
    }



    private fun manejarFallo(fallo: Throwable) {
        val errorApi = (fallo as? ErrorApiException)?.error

        when (errorApi) {
            is ErrorApi.Validacion -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Inactiva,
                        formulario = it.formulario.copy(
                            nombreError = errorApi.porCampo["nombre"],
                            precioError = errorApi.porCampo["precio"],
                            stockError = errorApi.porCampo["stock"]
                        )
                    )
                }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        operacion = ProductoUiState.Operacion.Fallida(
                            mensaje = mensajeDe(errorApi ?: fallo)
                        )
                    )
                }
            }
        }
    }

    private fun mensajeDe(error: Any): String = when (error) {
        is ErrorApi.NoEncontrado -> "El recurso solicitado no existe (404)."
        is ErrorApi.Conflicto -> "Conflicto: ${error.mensaje}"
        is ErrorApi.Servidor -> "El servidor presentó un problema (500). Intente nuevamente."
        is ErrorApi.SinConexion -> "No hay conexión de red disponible. Verifique su conexión."
        is ErrorApi.TiempoAgotado -> "Tiempo de espera agotado al conectar con el servidor."
        is Throwable -> error.message ?: "Ocurrió un error inesperado."
        else -> error.toString()
    }

    private fun limpiarFormulario() {
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(),
                operacion = ProductoUiState.Operacion.Inactiva
            )
        }
    }
}
