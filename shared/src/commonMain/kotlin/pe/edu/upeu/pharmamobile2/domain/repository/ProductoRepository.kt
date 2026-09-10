package pe.edu.upeu.pharmamobile2.domain.repository

import pe.edu.upeu.pharmamobile2.domain.model.Producto

interface ProductoRepository {
    suspend fun registrar(producto: Producto): Producto
    suspend fun listar(): List<Producto>
}
