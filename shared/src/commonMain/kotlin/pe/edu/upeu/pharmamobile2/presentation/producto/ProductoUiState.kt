package pe.edu.upeu.pharmamobile2.presentation.producto

import pe.edu.upeu.pharmamobile2.domain.model.Producto

data class FormularioProducto(
    val id: Long? = null,
    val nombre: String = "",
    val precio: String = "",
    val stock: String = "",
    val categoriaId: Long = 26L,
    val categoriaNombre: String = "Analgésicos",
    val nombreError: String? = null,
    val precioError: String? = null,
    val stockError: String? = null
) {
    val estaEnModoEdicion: Boolean get() = id != null && id > 0L
}

data class ProductoUiState(
    val fase: Fase = Fase.Cargando,
    val formulario: FormularioProducto = FormularioProducto(),
    val operacion: Operacion = Operacion.Inactiva,
    val mensajeExito: String? = null
) {
    sealed interface Fase {
        data object Cargando : Fase
        data object SinProductos : Fase
        data class ConProductos(
            val productos: List<Producto>
        ) : Fase
        data class Error(
            val mensaje: String
        ) : Fase
    }

    sealed interface Operacion {
        data object Inactiva : Operacion

        data class EnCurso(
            val tipo: Tipo
        ) : Operacion

        data class Fallida(
            val mensaje: String
        ) : Operacion

        enum class Tipo {
            Crear,
            Actualizar,
            Eliminar
        }
    }
}

typealias FaseProductos = ProductoUiState.Fase
