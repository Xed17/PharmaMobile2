package pe.edu.upeu.pharmamobile2.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.domain.result.ResultadoProductos

private val productosSimulados = listOf(
    Producto(id = 1, nombre = "Paracetamol", precio = 8.50, stock = 100),
    Producto(id = 2, nombre = "Ibuprofeno", precio = 12.00, stock = 50),
    Producto(id = 3, nombre = "Amoxicilina", precio = 18.50, stock = 20)
)

suspend fun obtenerProductos(): List<Producto> {
    delay(1_000)
    return productosSimulados
}

fun describirResultado(resultado: ResultadoProductos): String {
    return when (resultado) {
        ResultadoProductos.Cargando -> "Cargando productos..."
        is ResultadoProductos.Exito -> "Productos cargados: ${resultado.productos.size}"
        is ResultadoProductos.Error -> "Error: ${resultado.mensaje}"
    }
}


fun observarProductos(): Flow<List<Producto>> = flow {
    emit(emptyList())
    delay(1_000)
    emit(productosSimulados)
}


fun cargarProductos(): Flow<ResultadoProductos> = flow {
    emit(ResultadoProductos.Cargando)

    try {
        delay(1_000)
        emit(ResultadoProductos.Exito(productosSimulados))
    } catch (e: Exception) {
        emit(ResultadoProductos.Error(e.message ?: "Error desconocido"))
    }
}
