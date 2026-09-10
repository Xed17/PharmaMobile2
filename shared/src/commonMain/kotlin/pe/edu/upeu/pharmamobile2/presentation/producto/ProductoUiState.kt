package pe.edu.upeu.pharmamobile2.presentation.producto

import pe.edu.upeu.pharmamobile2.domain.model.Producto

data class ProductoUiState(
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val errorNombre: String? = null,
    val errorPrecio: String? = null,
    val errorStock: String? = null,
    val mensajeFormulario: String? = null,
    val esErrorFormulario: Boolean = false,
    val fase: FaseProductos = FaseProductos.Cargando
)

sealed interface FaseProductos {
    data object Cargando : FaseProductos
    data object SinProductos : FaseProductos
    data class ConProductos(val productos: List<Producto>) : FaseProductos
    data class Error(val mensaje: String) : FaseProductos
}
