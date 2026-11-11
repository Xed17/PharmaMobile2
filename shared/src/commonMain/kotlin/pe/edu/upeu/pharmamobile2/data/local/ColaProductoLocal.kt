package pe.edu.upeu.pharmamobile2.data.local

import app.cash.sqldelight.coroutines.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobil.db.Producto as Entidad
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.*
import pe.edu.upeu.pharmamobile2.domain.repository.ColaProductoRepository
import pe.edu.upeu.pharmamobile2.domain.sync.PoliticaReintento

class ColaProductoLocal(
    private val db: PharmaDb,
    private val contexto: ContextoCuenta,
    private val reloj: Reloj,
    private val ids: GeneradorIds,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ColaProductoRepository {
    init { require(contexto.clave.isNotBlank() && contexto.clave != "legacy_unassigned") }
    private val q = db.productoQueries
    private val cuenta get() = contexto.clave
    private val politica = PoliticaReintento()
    override fun observar(): Flow<List<RegistroEnvio>> = q.envios(cuenta).asFlow().mapToList(dispatcher).map { rows -> rows.map { it.registro() } }
    override suspend fun elegibles() = q.elegibles(cuenta, PoliticaReintento.MAX_INTENTOS.toLong(), reloj.ahoraMs()).executeAsList().map { it.registro() }
    override suspend fun porId(id: Long): RegistroEnvio? = fila(id)?.takeIf { it.pendiente == 1L }?.registro()
    private fun fila(id: Long) = q.porIdLocal(cuenta, id).executeAsOneOrNull()
    override suspend fun guardar(producto: Producto): RegistroEnvio = db.transactionWithResult {
        val id = q.siguienteIdLocal(cuenta).executeAsOneOrNull() ?: -1L
        q.insertar(cuenta,id,producto.nombre,producto.precio,producto.stock.toLong(),if(producto.activo) 1L else 0L,
            producto.categoriaId,producto.categoriaNombre,1,EstadoEnvio.PENDIENTE.name,ids.nuevo())
        fila(id)!!.registro()
    }
    override suspend fun iniciar(id: Long, ahoraMs: Long): RegistroEnvio? = db.transactionWithResult {
        q.iniciarEnvio(cuenta,id,PoliticaReintento.MAX_INTENTOS.toLong(),ahoraMs)
        if(q.cambios().executeAsOne() == 1L) fila(id)!!.also { historial(it,EstadoEnvio.ENVIANDO.name,null,null,ahoraMs) }.registro() else null
    }
    override suspend fun resolver(id: Long, resultado: ResultadoEnvio, ahoraMs: Long) {
        db.transaction {
            val row = fila(id) ?: error("No existe el envío de esta cuenta")
            require(row.estadoEnvio == EstadoEnvio.ENVIANDO.name)
            when(resultado) {
                is ResultadoEnvio.Exito -> {
                    require(resultado.producto.id > 0)
                    historial(row,EstadoEnvio.CONFIRMADO.name,null,null,ahoraMs)
                    ProductoLocal(db,contexto,dispatcher).confirmarRegistro(id,resultado.producto)
                }
                is ResultadoEnvio.Fallo -> {
                    val e = resultado.error
                    val estado = when {
                        row.intentos >= PoliticaReintento.MAX_INTENTOS -> EstadoEnvio.FALLIDA_AGOTADA
                        e.naturaleza == NaturalezaFallo.SESION -> EstadoEnvio.REQUIERE_SESION
                        e.naturaleza == NaturalezaFallo.PERMISOS -> EstadoEnvio.REQUIERE_PERMISO
                        e.efectoIncierto || e.naturaleza == NaturalezaFallo.TRANSITORIO -> EstadoEnvio.REVISION
                        else -> EstadoEnvio.FALLIDA_PERMANENTE
                    }
                    val proximo = if(e.naturaleza == NaturalezaFallo.TRANSITORIO) ahoraMs + politica.esperaMs((row.intentos - 1).toInt()) else null
                    q.resolverError(estado.name,e.codigo,e.mensajeSeguro,Json.encodeToString(e.campos),proximo,cuenta,id)
                    historial(row,estado.name,e.codigo,e.mensajeSeguro,ahoraMs)
                }
            }
        }
    }
    override suspend fun recuperarInterrumpidos() {
        db.transaction {
            q.envios(cuenta).executeAsList().filter { it.estadoEnvio == EstadoEnvio.ENVIANDO.name }.forEach { row ->
                val estado = if(row.intentos >= PoliticaReintento.MAX_INTENTOS) EstadoEnvio.FALLIDA_AGOTADA.name else EstadoEnvio.REVISION.name
                q.resolverError(estado,"INTERRUMPIDO","No se conoce el resultado del envío",null,null,cuenta,row.id)
                historial(row,estado,"INTERRUMPIDO","No se conoce el resultado del envío",reloj.ahoraMs())
            }
        }
    }
    override suspend fun autorizarReenvio(id: Long, ahoraMs: Long, aceptaRiesgo: Boolean) {
        db.transaction {
            val row = fila(id) ?: error("Envío inexistente")
            require(row.estadoEnvio in listOf(EstadoEnvio.REVISION.name,EstadoEnvio.REQUIERE_SESION.name,EstadoEnvio.REQUIERE_PERMISO.name))
            require(row.estadoEnvio != EstadoEnvio.REVISION.name || aceptaRiesgo) { "Reenviar puede duplicar el producto" }
            require(row.intentos < PoliticaReintento.MAX_INTENTOS)
            require(row.proximoIntentoEnMs?.let { ahoraMs >= it } != false)
            q.liberarEnvio(cuenta,id)
        }
    }
    override suspend fun corregirRechazado(id: Long, producto: Producto) {
        db.transaction {
            require(fila(id)?.estadoEnvio == EstadoEnvio.FALLIDA_PERMANENTE.name)
            q.corregirEnvio(producto.nombre,producto.precio,producto.stock.toLong(),if(producto.activo) 1L else 0L,
                producto.categoriaId,producto.categoriaNombre,cuenta,id)
        }
    }
    override suspend fun rehabilitarAgotado(id: Long, ahoraMs: Long, aceptaRiesgo: Boolean) {
        db.transaction {
            val row = fila(id) ?: error("Envío inexistente")
            require(row.estadoEnvio == EstadoEnvio.FALLIDA_AGOTADA.name && aceptaRiesgo)
            require(row.proximoIntentoEnMs?.let { ahoraMs >= it } != false)
            q.nuevaRevision(cuenta,id)
        }
    }
    override suspend fun descartarLocal(id: Long) {
        db.transaction {
            require(fila(id)?.pendiente == 1L)
            q.eliminarPorId(cuenta,id)
        }
    }
    private fun historial(row: Entidad, estado: String, codigo: String?, mensaje: String?, ahora: Long) {
        db.historialEnvioQueries.guardar(cuenta,requireNotNull(row.operacionLocalId),row.revision,row.intentos,estado,codigo,mensaje,ahora)
    }
    private fun Entidad.registro(): RegistroEnvio {
        val estado = EstadoEnvio.valueOf(estadoEnvio)
        val error = ultimoErrorCodigo?.let { codigo ->
            val naturaleza = when(estado) {
                EstadoEnvio.REQUIERE_SESION -> NaturalezaFallo.SESION
                EstadoEnvio.REQUIERE_PERMISO -> NaturalezaFallo.PERMISOS
                EstadoEnvio.FALLIDA_PERMANENTE -> NaturalezaFallo.PERMANENTE
                else -> NaturalezaFallo.TRANSITORIO
            }
            val campos = erroresCampos?.let { Json.decodeFromString<Map<String,String>>(it) }.orEmpty()
            FalloOperacion(naturaleza,codigo,ultimoError.orEmpty(),campos,estado == EstadoEnvio.REVISION || estado == EstadoEnvio.FALLIDA_AGOTADA)
        }
        return RegistroEnvio(Producto(id,nombre,precio,stock.toInt(),activo == 1L,categoriaId ?: 0L,categoriaNombre),
            estado,requireNotNull(operacionLocalId),revision.toInt(),intentos.toInt(),error,proximoIntentoEnMs)
    }
}
