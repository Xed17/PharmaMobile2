package pe.edu.upeu.pharmamobile2.domain.usecase

import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.Reloj
import pe.edu.upeu.pharmamobile2.domain.repository.*
import pe.edu.upeu.pharmamobile2.domain.port.DisparadorSincronizacion

class RevisarEnvioUseCase(private val cola: ColaProductoRepository, private val auth: AuthRepository,
    private val contexto: ContextoCuenta, private val reloj: Reloj, private val disparador: DisparadorSincronizacion) {
    private fun propietario() { require(auth.esActual(contexto)) { "La cuenta cambió. Abre de nuevo tus pendientes" } }
    private suspend fun escrituraRemota() {
        propietario()
        require(auth.sesionRemota(contexto)!=null) { "Inicia sesión con conexión antes de autorizar el envío" }
        require(auth.sesionActual.value?.puede("CATALOGO_ESCRIBIR")==true) { "No tienes permiso para escribir el catálogo" }
    }
    suspend operator fun invoke(id: Long, decision: DecisionEnvio, aceptaRiesgo: Boolean=false): Result<Unit> = resultado {
        propietario()
        val fila=requireNotNull(cola.porId(id)) { "Envío inexistente en esta cuenta" }
        when(decision) {
            DecisionEnvio.MANTENER -> Unit
            DecisionEnvio.DESCARTAR -> {
                require(fila.estado!=EstadoEnvio.ENVIANDO) { "Espera a que termine el envío" };cola.descartarLocal(id)
            }
            DecisionEnvio.REENVIAR -> {
                escrituraRemota();require(aceptaRiesgo) { "Debes aceptar el riesgo de crear un duplicado" }
                cola.autorizarReenvio(id,reloj.ahoraMs(),true);disparador.solicitar(MotivoSync.REVISION)
            }
            DecisionEnvio.REHABILITAR -> {
                escrituraRemota();require(aceptaRiesgo) { "Confirma que revisaste el servidor antes de rehabilitar" }
                cola.rehabilitarAgotado(id,reloj.ahoraMs(),true);disparador.solicitar(MotivoSync.REVISION)
            }
        }
    }
    suspend fun corregir(id: Long, producto: Producto): Result<Unit> = resultado {
        escrituraRemota()
        require(producto.nombre.isNotBlank() && producto.precio.isFinite() && producto.precio>0 && producto.stock>=0 && producto.categoriaId>0) { "Revisa nombre, precio, stock y categoría" }
        cola.corregirRechazado(id,producto.copy(id=id));disparador.solicitar(MotivoSync.REVISION)
    }
    private suspend fun resultado(accion: suspend ()->Unit): Result<Unit> = try {
        accion();Result.success(Unit)
    } catch(e: CancellationException) { throw e } catch(e: Exception) { Result.failure(e) }
}
