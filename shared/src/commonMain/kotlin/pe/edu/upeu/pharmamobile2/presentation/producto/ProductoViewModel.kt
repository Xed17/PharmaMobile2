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
import pe.edu.upeu.pharmamobile2.domain.model.BorradorProducto
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.repository.BorradorProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobile2.platform.formatearAntiguedad

import pe.edu.upeu.pharmamobile2.domain.model.CategoriaPredeterminada

class ProductoViewModel(
    private val observarProductos: ObservarProductosUseCase,
    private val sincronizarProductos: SincronizarProductosUseCase,
    private val registrarProducto: RegistrarProductoUseCase,
    private val actualizarProducto: ActualizarProductoUseCase,
    private val eliminarProducto: EliminarProductoUseCase,
    private val observarUltimaSincronizacion: ObservarUltimaSincronizacionUseCase? = null,
    private val borradorRepository: BorradorProductoRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductoUiState())
    val uiState: StateFlow<ProductoUiState> = _uiState.asStateFlow()

    init {
        iniciarObservacion()
        iniciarObservacionSincronizacion()
        iniciarObservacionBorrador()
        sincronizar()
    }

    private fun iniciarObservacion() {
        viewModelScope.launch {
            observarProductos().collect { productos ->
                val uiList = productos.map { it.toUi() }
                val pendientes = uiList.count { it.esPendiente }
                _uiState.update {
                    it.copy(
                        fase = if (uiList.isEmpty()) {
                            ProductoUiState.Fase.SinProductos
                        } else {
                            ProductoUiState.Fase.ConProductos(uiList)
                        },
                        totalPendientes = pendientes
                    )
                }
            }
        }
    }

    private fun iniciarObservacionSincronizacion() {
        observarUltimaSincronizacion?.let { useCase ->
            viewModelScope.launch {
                useCase().collect { timestamp ->
                    val texto = formatearAntiguedad(timestamp)
                    _uiState.update { it.copy(antiguedadSincronizacion = texto) }
                }
            }
        }
    }

    private fun iniciarObservacionBorrador() {
        val repo = borradorRepository ?: return
        viewModelScope.launch {
            repo.observar().collect { borrador ->
                if (borrador != null && _uiState.value.formulario.id == null) {
                    _uiState.update { state ->
                        if (state.formulario.nombre.isBlank() && state.formulario.precio.isBlank() && state.formulario.stock.isBlank()) {
                            state.copy(
                                formulario = state.formulario.copy(
                                    nombre = borrador.nombre,
                                    precio = borrador.precioTexto,
                                    stock = borrador.stockTexto,
                                    categoriaId = borrador.categoriaId ?: CategoriaPredeterminada.ID,
                                    categoriaNombre = borrador.categoriaNombre ?: CategoriaPredeterminada.NOMBRE
                                )
                            )
                        } else state
                    }
                }
            }
        }
    }

    private fun guardarBorrador() {
        val repo = borradorRepository ?: return
        val form = _uiState.value.formulario
        if (!form.estaEnModoEdicion) {
            viewModelScope.launch {
                repo.guardar(
                    BorradorProducto(
                        idEdicion = null,
                        nombre = form.nombre,
                        precioTexto = form.precio,
                        stockTexto = form.stock,
                        categoriaId = form.categoriaId,
                        categoriaNombre = form.categoriaNombre
                    )
                )
            }
        }
    }

    fun sincronizar() {
        if (_uiState.value.sincronizando) return
        viewModelScope.launch {
            _uiState.update { it.copy(sincronizando = true, mensajeSincronizacion = null) }
            try {
                sincronizarProductos()
                _uiState.update {
                    it.copy(
                        sincronizando = false,
                        mensajeSincronizacion = "Catálogo sincronizado"
                    )
                }
            } catch (e: Exception) {
                val mensaje = mensajeDe(e)
                _uiState.update {
                    it.copy(
                        sincronizando = false,
                        mensajeSincronizacion = mensaje
                    )
                }
            }
        }
    }

    fun actualizarNombre(valor: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(nombre = valor, nombreError = null))
        }
        guardarBorrador()
    }

    fun actualizarPrecio(valor: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(precio = valor, precioError = null))
        }
        guardarBorrador()
    }

    fun actualizarStock(valor: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(stock = valor, stockError = null))
        }
        guardarBorrador()
    }

    fun actualizarCategoria(id: Long, nombre: String) {
        _uiState.update {
            it.copy(formulario = it.formulario.copy(categoriaId = id, categoriaNombre = nombre))
        }
        guardarBorrador()
    }

    fun seleccionarParaEditar(producto: ProductoUi) {
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(
                    id = producto.id,
                    nombre = producto.nombre,
                    precio = producto.precioNumerico.toString(),
                    stock = producto.stock.toString(),
                    categoriaId = producto.categoriaId,
                    categoriaNombre = producto.categoriaNombre ?: "General"
                ),
                mensajeExito = null
            )
        }
    }

    fun seleccionarParaEditar(producto: Producto) {
        seleccionarParaEditar(producto.toUi())
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
                        mensajeExito = "Guardado en este equipo. Pendiente de envío"
                    )
                }
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
        is ErrorApi.NoAutorizado -> "Su sesión ha expirado o no es válida (401)."
        is ErrorApi.SinPermisos -> error.mensaje.ifBlank { "No tienes permiso para esta operación (403)." }
        is ErrorApi.Servidor -> "El servidor presentó un problema (500). Intente nuevamente."
        is ErrorApi.SinConexion -> "No hay conexión de red disponible. Verifique su conexión."
        is ErrorApi.TiempoAgotado -> "Tiempo de espera agotado al conectar con el servidor."
        is Throwable -> error.message ?: "Ocurrió un error inesperado."
        else -> error.toString()
    }

    fun limpiarFormulario() {
        _uiState.update {
            it.copy(
                formulario = FormularioProducto(),
                operacion = ProductoUiState.Operacion.Inactiva
            )
        }
        borradorRepository?.let { repo ->
            viewModelScope.launch { repo.borrar() }
        }
    }
}
