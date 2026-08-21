package pe.edu.upeu.pharmamobile2.domain

import pe.edu.upeu.pharmamobile2.domain.model.Producto

suspend fun recolectarProductos(onProductosRecibidos: (List<Producto>) -> Unit = {}) {
    observarProductos().collect { listaProductos ->
        println("Productos recibidos: $listaProductos")
        onProductosRecibidos(listaProductos)
    }
}

