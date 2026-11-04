package pe.edu.upeu.pharmamobile2.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import pe.edu.upeu.pharmamobile2.data.remote.dto.*
import pe.edu.upeu.pharmamobile2.domain.model.*

class ProductoApi(private val client: HttpClient, private val tokenPorDefecto: suspend () -> String? = { null }) {
    suspend fun listar(pagina: Int = 0, tamanio: Int = 20, ordenarPor: String = "id", direccion: String = "asc", token: String? = null): PaginaResponseDto<ProductoResponseDto> {
        val capturado = token ?: tokenPorDefecto()
        return client.get("api/v1/productos") {
            capturado?.let { bearerAuth(it) }
            parameter("pagina",pagina);parameter("tamanio",tamanio)
            parameter("ordenarPor",ordenarPor);parameter("direccion",direccion)
        }.body()
    }
    suspend fun obtenerProductos(token: String? = null): List<ProductoResponseDto> {
        val capturado = token ?: tokenPorDefecto()
        val resultado=mutableListOf<ProductoResponseDto>();val ids=mutableSetOf<Long>()
        var pagina=0;var totalPaginas: Int?=null;var totalElementos: Long?=null
        fun validar(condicion: Boolean) {
            if(!condicion) throw FalloOperacionException(FalloOperacion(NaturalezaFallo.PERMANENTE,"DATOS_INCONSISTENTES","La descarga del catálogo es inconsistente"))
        }
        while(true) {
            val actual=listar(pagina=pagina,token=capturado)
            validar(actual.pagina==pagina && actual.totalPaginas>=0 && actual.totalElementos>=0)
            if(totalPaginas==null) { totalPaginas=actual.totalPaginas;totalElementos=actual.totalElementos }
            validar(actual.totalPaginas==totalPaginas && actual.totalElementos==totalElementos)
            validar(actual.contenido.isNotEmpty() || actual.ultima)
            validar(actual.ultima==(pagina+1>=actual.totalPaginas))
            actual.contenido.forEach { validar(it.id>0 && ids.add(it.id));resultado.add(it) }
            if(actual.ultima) { validar(resultado.size.toLong()==actual.totalElementos);return resultado }
            pagina++
        }
    }
    suspend fun obtener(id: Long, token: String? = null): ProductoResponseDto {
        val capturado=token ?: tokenPorDefecto()
        return client.get("api/v1/productos/$id") { capturado?.let { bearerAuth(it) } }.body()
    }
    suspend fun crear(request: ProductoRequestDto, token: String? = null): ProductoResponseDto {
        val capturado=token ?: tokenPorDefecto()
        return client.post("api/v1/productos") { capturado?.let { bearerAuth(it) };setBody(request) }.body()
    }
    suspend fun actualizar(id: Long, request: ProductoRequestDto, token: String? = null): ProductoResponseDto {
        val capturado=token ?: tokenPorDefecto()
        return client.put("api/v1/productos/$id") { capturado?.let { bearerAuth(it) };setBody(request) }.body()
    }
    suspend fun eliminar(id: Long, token: String? = null) {
        val capturado=token ?: tokenPorDefecto()
        client.delete("api/v1/productos/$id") { capturado?.let { bearerAuth(it) } }
    }
}
