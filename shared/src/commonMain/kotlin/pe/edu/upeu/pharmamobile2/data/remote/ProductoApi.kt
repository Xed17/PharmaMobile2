package pe.edu.upeu.pharmamobile2.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import pe.edu.upeu.pharmamobile2.data.remote.dto.PaginaProductosDto
import pe.edu.upeu.pharmamobile2.data.remote.dto.ProductoDto

class ProductoApi(
    private val client: HttpClient
) {
    suspend fun obtenerProductos(
        pagina: Int = 0,
        tamanio: Int = 20,
        ordenarPor: String = "id",
        direccion: String = "asc"
    ): List<ProductoDto> {
        return runCatching {
            client.get("api/v1/productos") {
                parameter("pagina", pagina)
                parameter("tamanio", tamanio)
                parameter("ordenarPor", ordenarPor)
                parameter("direccion", direccion)
            }.body<PaginaProductosDto>().contenido
        }.recoverCatching {
            client.get("api/v1/productos").body<List<ProductoDto>>()
        }.getOrThrow()
    }
}
