package pe.edu.upeu.pharmamobile2.navigation

sealed class Screen {
    data object Inicio : Screen()
    data object Productos : Screen()
    data class DetalleProducto(val productoId: Long) : Screen()
    data object RevisionEnvios : Screen()
    data object Clientes : Screen()
    data object Pedidos : Screen()
    data object AcercaDe : Screen()
}

fun tituloPantalla(screen: Screen): String = when (screen) {
    is Screen.Inicio -> "Inicio"
    is Screen.Productos -> "Productos"
    is Screen.DetalleProducto -> "Detalle de Producto"
    is Screen.RevisionEnvios -> "Revisión de Envíos"
    is Screen.Clientes -> "Clientes"
    is Screen.Pedidos -> "Pedidos"
    is Screen.AcercaDe -> "Acerca de"
}
