package pe.edu.upeu.pharmamobile2.domain.repository

import pe.edu.upeu.pharmamobile2.domain.model.*

interface ProductoRemotoRepository {
    suspend fun crear(producto: Producto): ResultadoEnvio
    suspend fun listarCompleto(): Result<List<Producto>>
}
