package pe.edu.upeu.pharmamobile2.navigation

sealed class Screen {
    data object Inicio : Screen()
    data object Productos : Screen()
    data object Clientes : Screen()
    data object Pedidos : Screen()
}

fun tituloPantalla(screen: Screen): String = when (screen) {
    is Screen.Inicio -> "Inicio"
    is Screen.Productos -> "Productos"
    is Screen.Clientes -> "Clientes"
    is Screen.Pedidos -> "Pedidos"
}