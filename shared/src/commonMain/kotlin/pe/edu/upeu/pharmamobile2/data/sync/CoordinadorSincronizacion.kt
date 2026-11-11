package pe.edu.upeu.pharmamobile2.data.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobile2.data.notificacion.PoliticaAvisos
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.*
import pe.edu.upeu.pharmamobile2.domain.repository.*
import pe.edu.upeu.pharmamobile2.domain.usecase.*

class CoordinadorSincronizacion(private val scope: CoroutineScope,private val enviar: EnviarPendientesUseCase,
    private val descargar: DescargarCatalogoUseCase,private val cola: ColaProductoRepository,
    private val descargaLocal: DescargaRepository,private val auth: AuthRepository,private val contexto: ContextoCuenta,
    private val disparador: DisparadorSincronizacion,private val monitor: MonitorConectividad,private val notificador: NotificadorSincronizacion? = null) : SincronizadorProductos {
    private val puerta=Mutex()
    private var activo: Deferred<ResumenSincronizacion>?=null
    private var pendiente: MotivoSync?=null
    private val escuchas=mutableListOf<Job>()
    private var iniciado=false
    private var detenido=false
    private var ultimoAviso: ResumenSincronizacion?=null
    private val _actividad=MutableStateFlow(false)
    override val actividad=_actividad.asStateFlow()
    private val _resultados=MutableSharedFlow<ResumenSincronizacion>(extraBufferCapacity=16)
    override val resultados=_resultados.asSharedFlow()

    override fun iniciar() {
        if(iniciado || detenido) return
        iniciado=true
        escuchas += scope.launch {
            disparador.eventos.collect { motivo ->
                try { sincronizar(motivo) } catch(e: CancellationException) {
                    if(!currentCoroutineContext().isActive) throw e
                }
            }
        }
        escuchas += scope.launch {
            var anterior: EstadoConectividad?=null
            monitor.estado.collect { estado ->
                val previo=anterior;anterior=estado
                if(estado==EstadoConectividad.DISPONIBLE && previo!=null && previo!=EstadoConectividad.DISPONIBLE)
                    disparador.solicitar(MotivoSync.CONECTIVIDAD)
                else if(estado!=EstadoConectividad.DISPONIBLE && previo==EstadoConectividad.DISPONIBLE)
                    cancelarCiclo()
            }
        }
        disparador.solicitar(MotivoSync.APERTURA)
    }
    override suspend fun sincronizar(motivo: MotivoSync): ResumenSincronizacion {
        val trabajo=puerta.withLock {
            if(detenido) throw CancellationException("La sincronización se detuvo")
            val actual=activo
            if(actual!=null && !actual.isCompleted) {
                if(actual.isActive) {
                    // An explicit GET recovery must not be lost to a later ordinary refresh.
                    if(pendiente!=MotivoSync.REINTENTO_DESCARGA) pendiente=motivo
                }
                actual
            } else scope.async { ejecutarCiclos(motivo) }.also { activo=it }
        }
        return trabajo.await()
    }
    override suspend fun reintentarDescarga() = sincronizar(MotivoSync.REINTENTO_DESCARGA)
    private suspend fun ejecutarCiclos(inicial: MotivoSync): ResumenSincronizacion {
        val ejecutando=currentCoroutineContext().job
        var motivo=inicial
        var enviadas=0
        try {
            _actividad.value=true
            while(true) {
                val resultado=ciclo(motivo)
                enviadas+=resultado.enviadas
                _resultados.emit(resultado)
                avisar(resultado)
                val siguiente=puerta.withLock {
                    val siguiente=pendiente;pendiente=null
                    if(siguiente==null && activo===ejecutando) { activo=null;_actividad.value=false }
                    siguiente
                }
                if(siguiente==null) return resultado.copy(enviadas=enviadas)
                motivo=siguiente
            }
        } finally {
            withContext(NonCancellable) {
                puerta.withLock {
                    if(activo===ejecutando) { activo=null;pendiente=null;_actividad.value=false }
                }
            }
        }
    }
    /** Solo avisa si hubo novedades; `enviadas` es por ciclo, por eso no se compara con el aviso anterior. */
    private suspend fun avisar(resultado: ResumenSincronizacion) {
        val previo=ultimoAviso?.copy(enviadas=0)
        ultimoAviso=resultado
        if(!PoliticaAvisos.debeNotificar(previo,resultado,habilitada=true)) return
        try { notificador?.publicar(contexto,resultado) } catch (e: Exception) { if(e is CancellationException) throw e }
    }
    private suspend fun vigente() {
        currentCoroutineContext().ensureActive()
        if(!auth.esActual(contexto)) throw CancellationException("Contexto de cuenta obsoleto")
    }
    private suspend fun ciclo(motivo: MotivoSync): ResumenSincronizacion {
        vigente()
        cola.recuperarInterrumpidos()
        if(!monitor.disponible()) return resumen(0,false)
        if(auth.sesionRemota(contexto)==null) {
            if((auth.acceso.value as? AccesoSesion.Disponible)?.modo==ModoAcceso.REMOTO) auth.notificar401()
            return resumen(0,false)
        }
        val subida=enviar()
        vigente()
        if(subida.detenerPorSesion || subida.detenerPorRed) return resumen(subida.enviadas,false)
        val completa=descargar(reiniciarAgotada=motivo==MotivoSync.REINTENTO_DESCARGA)
        vigente()
        return resumen(subida.enviadas,completa)
    }
    private suspend fun resumen(enviadas: Int,completa: Boolean): ResumenSincronizacion {
        val filas=cola.observar().first()
        return ResumenSincronizacion(enviadas,
            filas.count { it.estado==EstadoEnvio.PENDIENTE || it.estado==EstadoEnvio.ENVIANDO },
            filas.count { it.estado==EstadoEnvio.FALLIDA_PERMANENTE || it.estado==EstadoEnvio.FALLIDA_AGOTADA },
            filas.count { it.estado==EstadoEnvio.REVISION },
            filas.count { it.estado==EstadoEnvio.REQUIERE_SESION || it.estado==EstadoEnvio.REQUIERE_PERMISO },
            completa,descargaLocal.observarUltimaSync().first())
    }
    private suspend fun cancelarCiclo() {
        val trabajo=puerta.withLock { pendiente=null;activo?.also { it.cancel() } }
        trabajo?.join()
    }
    override suspend fun detener() {
        detenido=true
        escuchas.forEach { it.cancel() }
        cancelarCiclo()
        escuchas.joinAll();escuchas.clear()
        disparador.cerrar()
        try { notificador?.cancelar(contexto) } catch (e: Exception) { if(e is CancellationException) throw e }
    }
}
