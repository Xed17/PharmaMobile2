package pe.edu.upeu.pharmamobile2.testutil

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.FakeAlmacenSeguro
import pe.edu.upeu.pharmamobile2.data.local.*
import pe.edu.upeu.pharmamobile2.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRemotoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.EnviarPendientesUseCase

class RemotoPrueba : ProductoRemotoRepository {
    var llamadas=0
    var lecturas=0
    var descarga: suspend () -> Result<List<Producto>> = { Result.success(emptyList()) }
    var respuesta: suspend (Producto)->ResultadoEnvio = { ResultadoEnvio.Exito(it.copy(id=100L)) }
    override suspend fun crear(producto: Producto): ResultadoEnvio { llamadas++;return respuesta(producto) }
    override suspend fun listarCompleto(): Result<List<Producto>> { lecturas++;return descarga() }
}
class EscenarioSync private constructor(val driver: SqlDriver,val client: HttpClient,val db: PharmaDb,
    val auth: AuthRepositorioRest,val contexto: ContextoCuenta,val reloj: RelojFalso,val dispatcher: CoroutineDispatcher) {
    val cola=ColaProductoLocal(db,contexto,reloj,GeneradorIdsSecuencial(),dispatcher)
    val local=ProductoLocal(db,contexto,dispatcher)
    val remoto=RemotoPrueba()
    val enviar=EnviarPendientesUseCase(cola,remoto,auth,contexto,reloj)
    fun cerrar() { client.close();driver.close() }
    companion object {
        suspend fun crear(dispatcher: CoroutineDispatcher): EscenarioSync {
            val client=HttpClient(MockEngine { respond("""{"token":"token-A","username":"A","nombreCompleto":"Prueba","permisos":["CATALOGO_LEER","CATALOGO_ESCRIBIR"]}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) }) {
                install(ContentNegotiation) { json() };defaultRequest { contentType(ContentType.Application.Json) }
            }
            val reloj=RelojFalso(1000);val auth=AuthRepositorioRest(client,FakeAlmacenSeguro(),reloj)
            auth.iniciarSesion(Credenciales("A","prueba")).getOrThrow()
            val contexto=(auth.acceso.value as AccesoSesion.Disponible).contexto
            val driver=crearDriverPruebas()
            return EscenarioSync(driver,client,PharmaDb(driver),auth,contexto,reloj,dispatcher)
        }
    }
}
