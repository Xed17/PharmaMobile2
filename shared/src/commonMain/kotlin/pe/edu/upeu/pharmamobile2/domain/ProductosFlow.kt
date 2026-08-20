package pe.edu.upeu.pharmamobile2.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.pharmamobile2.domain.model.Producto

val productosRemotos: List<Producto> = listOf(
    Producto(1, "Paracetamol", 5.0, 20),
    Producto(2, "Ibuprofeno", 8.5, 15),
    Producto(3, "Amoxicilina", 15.0, 10)
)

fun observarProductos(): Flow<List<Producto>> = flow {
    emit(emptyList())
    delay(1000)
    emit(productosRemotos)
}

suspend fun recolectarProductos(onProductosRecibidos: (List<Producto>) -> Unit = {}) {
    observarProductos().collect { listaProductos ->
        println("Productos recibidos: $listaProductos")
        onProductosRecibidos(listaProductos)
    }
}
