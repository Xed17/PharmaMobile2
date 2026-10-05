package pe.edu.upeu.pharmamobile2.domain

import pe.edu.upeu.pharmamobile2.domain.model.Producto

val productosEjemplo: List<Producto> = listOf(
    Producto(1L, "Paracetamol", 5.0, 20),
    Producto(2L, "Ibuprofeno", 8.5, 0),
    Producto(3L, "Amoxicilina", 15.0, 10)
)

fun filtrarProductosDisponibles(lista: List<Producto> = productosEjemplo): List<Producto> {
    return lista.filter { it.stock > 0 }
}

fun obtenerNombresProductos(lista: List<Producto> = productosEjemplo): List<String> {
    return lista.map { it.nombre }
}

fun buscarProductoPorId(id: Long, lista: List<Producto> = productosEjemplo): Producto? {
    return lista.find { it.id == id }
}

fun buscarProductoPorId(id: Int, lista: List<Producto> = productosEjemplo): Producto? {
    return buscarProductoPorId(id.toLong(), lista)
}

fun obtenerMensajeProductoBuscado(id: Long, lista: List<Producto> = productosEjemplo): String {
    val producto = buscarProductoPorId(id, lista)
    return producto?.nombre ?: "Producto no encontrado"
}

fun obtenerMensajeProductoBuscado(id: Int, lista: List<Producto> = productosEjemplo): String {
    return obtenerMensajeProductoBuscado(id.toLong(), lista)
}
