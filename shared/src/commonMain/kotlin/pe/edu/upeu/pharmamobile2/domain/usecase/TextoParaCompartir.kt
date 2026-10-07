package pe.edu.upeu.pharmamobile2.domain.usecase

import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.platform.formatearSoles

fun Producto.comoTextoParaCompartir(): String {
    return "$nombre - ${formatearSoles(precio)} - Stock: $stock"
}
