package pe.edu.upeu.pharmamobile2.data.mapper

import pe.edu.upeu.pharmamobile2.data.remote.dto.ProductoDto
import pe.edu.upeu.pharmamobile2.domain.model.Producto

fun ProductoDto.toDomain(): Producto =
    Producto(
        id = id,
        nombre = nombre,
        precio = precio,
        stock = stock,
        activo = estado
    )
