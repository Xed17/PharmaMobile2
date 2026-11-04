package pe.edu.upeu.pharmamobile2.data.repository

import pe.edu.upeu.pharmamobile2.data.remote.*
import pe.edu.upeu.pharmamobile2.data.mapper.*
import pe.edu.upeu.pharmamobile2.domain.error.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRemotoRepository

class ProductoRemotoRest(private val api: ProductoApi, private val autorizador: AutorizadorSolicitudes, private val contexto: ContextoCuenta) : ProductoRemotoRepository {
    override suspend fun crear(producto: Producto): ResultadoEnvio = ejecutarLlamada {
        autorizador.conCredencial(contexto) { token -> api.crear(producto.toRequest(26L),token).toDomain() }
    }.fold({ ResultadoEnvio.Exito(it) }, { ResultadoEnvio.Fallo(traducir(it,true)) })
    override suspend fun listarCompleto(): Result<List<Producto>> = ejecutarLlamada {
        autorizador.conCredencial(contexto) { token -> api.obtenerProductos(token).map { it.toDomain() } }
    }.fold({ Result.success(it) }, { Result.failure(FalloOperacionException(traducir(it,false))) })
    private fun traducir(error: Throwable, creacion: Boolean): FalloOperacion {
        if(error is FalloOperacionException) return error.fallo.copy(efectoIncierto = error.fallo.efectoIncierto || creacion)
        val apiError=(error as? ErrorApiException)?.error
        val naturaleza=when(apiError) {
            ErrorApi.SinConexion,ErrorApi.TiempoAgotado,ErrorApi.Servidor -> NaturalezaFallo.TRANSITORIO
            ErrorApi.NoAutorizado -> NaturalezaFallo.SESION
            is ErrorApi.SinPermisos -> NaturalezaFallo.PERMISOS
            else -> NaturalezaFallo.PERMANENTE
        }
        val codigo=when(apiError) {
            ErrorApi.SinConexion -> "SIN_RED"
            ErrorApi.TiempoAgotado -> "TIMEOUT"
            ErrorApi.Servidor -> "HTTP_500"
            ErrorApi.NoAutorizado -> "HTTP_401"
            is ErrorApi.SinPermisos -> "HTTP_403"
            is ErrorApi.Validacion -> "HTTP_400"
            ErrorApi.NoEncontrado -> "HTTP_404"
            is ErrorApi.Conflicto -> "HTTP_409"
            ErrorApi.DatosInvalidos -> "DATOS_INVALIDOS"
            else -> "ERROR_INTERNO"
        }
        val mensaje=when(apiError) {
            is ErrorApi.Conflicto -> apiError.mensaje
            is ErrorApi.SinPermisos -> apiError.mensaje
            is ErrorApi.Validacion -> "Revisa los campos del registro"
            else -> when(naturaleza) {
                NaturalezaFallo.SESION -> "Inicia sesión para enviar los cambios"
                NaturalezaFallo.TRANSITORIO -> "No se pudo completar la comunicación con el servidor"
                else -> "No se pudo completar la operación"
            }
        }
        val incierto=creacion && (naturaleza==NaturalezaFallo.TRANSITORIO || apiError==ErrorApi.DatosInvalidos || apiError==ErrorApi.Inesperado)
        return FalloOperacion(naturaleza,codigo,mensaje,(apiError as? ErrorApi.Validacion)?.porCampo.orEmpty(),incierto)
    }
}
