package pe.edu.upeu.pharmamobile2.data.local

import kotlinx.coroutines.flow.Flow
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.repository.DescargaRepository

class DescargaLocal(private val db: PharmaDb, private val contexto: ContextoCuenta) : DescargaRepository {
    init { require(contexto.clave.isNotBlank() && contexto.clave != "legacy_unassigned") }
    private val q = db.descargaQueries
    private val local = ProductoLocal(db,contexto)
    override suspend fun actual(): CadenaDescarga? = q.obtener(contexto.clave).executeAsOneOrNull()?.let {
        val estado=EstadoCadena.valueOf(it.estado)
        val error=it.ultimoErrorCodigo?.let { codigo ->
            val tipo=when(codigo.removePrefix("HTTP_")) {
                "401" -> NaturalezaFallo.SESION
                "403" -> NaturalezaFallo.PERMISOS
                else -> if(estado == EstadoCadena.BLOQUEADA) NaturalezaFallo.PERMANENTE else NaturalezaFallo.TRANSITORIO
            }
            FalloOperacion(tipo,codigo,it.ultimoError.orEmpty())
        }
        CadenaDescarga(it.cadenaId,estado,it.intentos.toInt(),it.proximoEnMs,error)
    }
    override suspend fun guardar(cadena: CadenaDescarga) {
        require(cadena.intentos in 0..5)
        q.guardar(contexto.clave,cadena.id,cadena.estado.name,cadena.intentos.toLong(),cadena.proximoEnMs,
            if(cadena.estado == EstadoCadena.AGOTADA) cadena.proximoEnMs?.minus(16000L) else null,
            cadena.ultimoFallo?.codigo,cadena.ultimoFallo?.mensajeSeguro)
    }
    override suspend fun conciliarCompleta(productos: List<Producto>, ahoraMs: Long) {
        require(productos.all { it.id > 0 } && productos.map { it.id }.distinct().size == productos.size)
        db.transaction {
            local.reemplazarSincronizados(productos)
            local.guardarUltimaSincronizacion(ahoraMs)
            q.obtener(contexto.clave).executeAsOneOrNull()?.let { cadena ->
                q.guardar(contexto.clave,cadena.cadenaId,EstadoCadena.COMPLETA.name,cadena.intentos,null,null,null,null)
            }
        }
    }
    override fun observarUltimaSync(): Flow<Long?> = local.observarUltimaSincronizacion()
}
