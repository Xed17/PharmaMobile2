package pe.edu.upeu.pharmamobile2.data.mapper

import pe.edu.upeu.pharmamobile2.data.remote.dto.ProductoRequestDto
import pe.edu.upeu.pharmamobile2.data.remote.dto.ProductoResponseDto
import pe.edu.upeu.pharmamobile2.domain.model.Producto

fun ProductoResponseDto.toDomain(): Producto = Producto(
    id = id,
    nombre = nombre,
    precio = precio,
    stock = stock,
    activo = estado,
    categoriaId = categoriaId ?: 26L,
    categoriaNombre = categoriaNombre ?: "General"
)

fun Producto.toRequest(
    categoriaPorDefecto: Long = 26L
): ProductoRequestDto = ProductoRequestDto(
    nombre = nombre,
    precio = precio,
    stock = stock,
    estado = activo,
    categoriaId = if (categoriaId > 0L) categoriaId else categoriaPorDefecto
)
