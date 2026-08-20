package pe.edu.upeu.pharmamobile2.demo

import pe.edu.upeu.pharmamobile2.domain.result.ResultadoProductos

fun mostrarResultado(resultado: ResultadoProductos) {
    when (resultado) {
        ResultadoProductos.Cargando -> {
            println(
                "Cargando Productos"
            )
        }
        is ResultadoProductos.Exito -> {
            println(
                "Productos encontrados: ${resultado.productos.size}"
            )
        }
        is ResultadoProductos.Error -> {
            println(
                "Error: ${resultado.mensaje}"
            )
        }
    }
}