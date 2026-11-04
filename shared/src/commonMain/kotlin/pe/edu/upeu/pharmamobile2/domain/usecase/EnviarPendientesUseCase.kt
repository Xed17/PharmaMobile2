package pe.edu.upeu.pharmamobile2.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.Reloj
import pe.edu.upeu.pharmamobile2.domain.repository.*

data class ResumenEnvios(val enviadas: Int, val detenerPorSesion: Boolean = false,
    val detenerPorRed: Boolean = false, val detenerPorPermisos: Boolean = false)

class EnviarPendientesUseCase(private val cola: ColaProductoRepository, private val remoto: ProductoRemotoRepository,
    private val auth: AuthRepository, private val contexto: ContextoCuenta, private val reloj: Reloj) {
    private val mutex = Mutex()
    suspend operator fun invoke(): ResumenEnvios = mutex.withLock {
        fun vigente() {
            if(!auth.esActual(contexto)) throw CancellationException("La cuenta cambió")
        }
        vigente()
        if(auth.sesionRemota(contexto)==null) return@withLock ResumenEnvios(0,detenerPorSesion=true)
        if(auth.sesionActual.value?.puede("CATALOGO_ESCRIBIR")!=true)
            return@withLock ResumenEnvios(0,detenerPorPermisos=true)
        var enviadas=0
        for(fila in cola.elegibles()) {
            vigente()
            if(auth.sesionRemota(contexto)==null) return@withLock ResumenEnvios(enviadas,detenerPorSesion=true)
            val iniciada=cola.iniciar(fila.producto.id,reloj.ahoraMs()) ?: continue
            val resultado=try {
                remoto.crear(iniciada.producto)
            } catch(e: CancellationException) { throw e
            } catch(e: Exception) {
                ResultadoEnvio.Fallo(FalloOperacion(NaturalezaFallo.PERMANENTE,"ERROR_INTERNO","El envío necesita revisión",efectoIncierto=true))
            }
            vigente()
            cola.resolver(iniciada.producto.id,resultado,reloj.ahoraMs())
            when(resultado) {
                is ResultadoEnvio.Exito -> enviadas++
                is ResultadoEnvio.Fallo -> {
                    val error=resultado.error
                    when(error.naturaleza) {
                        NaturalezaFallo.SESION -> {
                            auth.notificar401()
                            return@withLock ResumenEnvios(enviadas,detenerPorSesion=true)
                        }
                        NaturalezaFallo.PERMISOS -> {
                            auth.notificar403(error.mensajeSeguro)
                            return@withLock ResumenEnvios(enviadas,detenerPorPermisos=true)
                        }
                        else -> if(error.naturaleza==NaturalezaFallo.TRANSITORIO || error.efectoIncierto)
                            return@withLock ResumenEnvios(enviadas,detenerPorRed=true)
                    }
                }
            }
        }
        ResumenEnvios(enviadas)
    }
}
