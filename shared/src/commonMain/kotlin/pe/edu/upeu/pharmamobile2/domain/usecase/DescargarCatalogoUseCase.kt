package pe.edu.upeu.pharmamobile2.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.*
import pe.edu.upeu.pharmamobile2.domain.repository.*
import pe.edu.upeu.pharmamobile2.domain.sync.PoliticaReintento

class DescargarCatalogoUseCase(private val local: DescargaRepository, private val remoto: ProductoRemotoRepository,
    private val auth: AuthRepository, private val contexto: ContextoCuenta, private val reloj: Reloj,
    private val espera: Espera, private val ids: GeneradorIds) {
    private val mutex=Mutex()
    private val politica=PoliticaReintento()
    private suspend fun vigente() {
        currentCoroutineContext().ensureActive()
        if(!auth.esActual(contexto)) throw CancellationException("Contexto de cuenta obsoleto")
    }
    private fun nueva()=CadenaDescarga(ids.nuevo(),EstadoCadena.NUEVA,0,null,null)
    suspend operator fun invoke(reiniciarAgotada: Boolean = false): Boolean = mutex.withLock {
        vigente()
        if(auth.sesionRemota(contexto)==null) return@withLock false
        var cadena=try { local.actual() ?: nueva() } catch(e: CancellationException) { throw e } catch(e: Exception) {
            val bloqueada=CadenaDescarga(ids.nuevo(),EstadoCadena.BLOQUEADA,0,reloj.ahoraMs()+16000,
                FalloOperacion(NaturalezaFallo.PERMANENTE,"METADATOS_INVALIDOS","Revisa el estado de la descarga"))
            local.guardar(bloqueada)
            return@withLock false
        }
        if(cadena.estado==EstadoCadena.COMPLETA) cadena=nueva()
        if(cadena.estado==EstadoCadena.AGOTADA || cadena.estado==EstadoCadena.BLOQUEADA) {
            val ahora=reloj.ahoraMs()
            val proximo=cadena.proximoEnMs
            if(proximo!=null && proximo>ahora && proximo-ahora>16000) {
                cadena=cadena.copy(proximoEnMs=ahora+16000)
                local.guardar(cadena)
            }
            if(!reiniciarAgotada || cadena.proximoEnMs?.let { ahora<it } == true) return@withLock false
            cadena=nueva() // Explicit recovery of a GET chain never changes any POST row.
        }
        if(cadena.estado==EstadoCadena.EN_CURSO) {
            if(cadena.intentos>=PoliticaReintento.MAX_INTENTOS) {
                local.guardar(cadena.copy(estado=EstadoCadena.AGOTADA,proximoEnMs=reloj.ahoraMs()+16000))
                return@withLock false
            }
            cadena=cadena.copy(estado=EstadoCadena.ESPERA,
                proximoEnMs=reloj.ahoraMs()+politica.esperaMs((cadena.intentos-1).coerceAtLeast(0)))
            local.guardar(cadena)
        }
        while(cadena.intentos<PoliticaReintento.MAX_INTENTOS) {
            vigente()
            val demora=cadena.proximoEnMs?.let { (it-reloj.ahoraMs()).coerceIn(0L,16000L) } ?: 0L
            if(demora>0) espera.esperar(demora)
            vigente()
            if(auth.sesionRemota(contexto)==null) return@withLock false
            cadena=cadena.copy(estado=EstadoCadena.EN_CURSO,intentos=cadena.intentos+1,proximoEnMs=null)
            local.guardar(cadena)
            val resultado=remoto.listarCompleto()
            vigente()
            if(resultado.isSuccess) {
                local.conciliarCompleta(resultado.getOrThrow(),reloj.ahoraMs())
                return@withLock true
            }
            val causa=resultado.exceptionOrNull()!!
            if(causa is CancellationException) throw causa
            val fallo=(causa as? FalloOperacionException)?.fallo
                ?: FalloOperacion(NaturalezaFallo.PERMANENTE,"ERROR_INTERNO","No se pudo actualizar el catálogo")
            val agotada=cadena.intentos>=PoliticaReintento.MAX_INTENTOS
            val transitorio=fallo.naturaleza==NaturalezaFallo.TRANSITORIO && !fallo.efectoIncierto
            val estado=when { agotada -> EstadoCadena.AGOTADA;transitorio -> EstadoCadena.ESPERA;else -> EstadoCadena.BLOQUEADA }
            val pausa=if(estado==EstadoCadena.ESPERA) politica.esperaMs(cadena.intentos-1) else 16000L
            cadena=cadena.copy(estado=estado,proximoEnMs=reloj.ahoraMs()+pausa,ultimoFallo=fallo)
            local.guardar(cadena)
            if(fallo.naturaleza==NaturalezaFallo.SESION) auth.notificar401()
            if(fallo.naturaleza==NaturalezaFallo.PERMISOS) auth.notificar403(fallo.mensajeSeguro)
            if(estado!=EstadoCadena.ESPERA) return@withLock false
        }
        false
    }
}
