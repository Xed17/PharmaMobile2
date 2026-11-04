package pe.edu.upeu.pharmamobile2.presentation.sincronizacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.edu.upeu.pharmamobile2.domain.model.*
import pe.edu.upeu.pharmamobile2.domain.port.EstadoPermiso
import pe.edu.upeu.pharmamobile2.domain.port.GestorPermisos
import pe.edu.upeu.pharmamobile2.domain.port.Permiso
import pe.edu.upeu.pharmamobile2.domain.port.Reloj
import pe.edu.upeu.pharmamobile2.domain.port.SincronizadorProductos
import pe.edu.upeu.pharmamobile2.domain.repository.ColaProductoRepository
import pe.edu.upeu.pharmamobile2.domain.repository.DescargaRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.RevisarEnvioUseCase
import pe.edu.upeu.pharmamobile2.platform.formatearAntiguedad

class SincronizacionViewModel(
    private val cola: ColaProductoRepository,
    private val descarga: DescargaRepository,
    private val coordinador: SincronizadorProductos,
    private val revisarEnvio: RevisarEnvioUseCase,
    private val reloj: Reloj,
    private val gestorPermisos: GestorPermisos? = null,
    iniciarTicker: Boolean = true
) : ViewModel() {

    private val tick = MutableStateFlow(reloj.ahoraMs())
    private val _permisoState = MutableStateFlow<EstadoPermiso>(EstadoPermiso.Denegado)
    private val _permisoDescartado = MutableStateFlow(false)

    private val baseSyncFlow = combine(
        cola.observar(),
        descarga.observarUltimaSync(),
        coordinador.actividad,
        tick
    ) { filas, ultimaSync, enCurso, ahora ->
        val pendientes = filas.count { it.estado == EstadoEnvio.PENDIENTE || it.estado == EstadoEnvio.ENVIANDO }
        val fallidas = filas.count { it.estado == EstadoEnvio.FALLIDA_PERMANENTE || it.estado == EstadoEnvio.FALLIDA_AGOTADA }
        val revision = filas.count { it.estado == EstadoEnvio.REVISION }
        val bloqueadas = filas.count { it.estado == EstadoEnvio.REQUIERE_SESION || it.estado == EstadoEnvio.REQUIERE_PERMISO }
        val antiguedad = formatearAntiguedad(ultimaSync, ahora)
        val cadena = descarga.actual()
        val puedeReintentar = cadena?.estado == EstadoCadena.AGOTADA &&
            (cadena.proximoEnMs == null || ahora >= cadena.proximoEnMs)

        SincronizacionUiState(
            enCurso = enCurso,
            pendientes = pendientes,
            fallidas = fallidas,
            revision = revision,
            bloqueadas = bloqueadas,
            ultimaSync = ultimaSync,
            antiguedad = antiguedad,
            puedeReintentarDescarga = puedeReintentar
        )
    }

    val estado: StateFlow<SincronizacionUiState> = combine(
        baseSyncFlow,
        _permisoState,
        _permisoDescartado
    ) { base, perm, descartado ->
        base.copy(
            estadoPermisoAvisos = perm,
            avisoPermisoDescartado = descartado
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SincronizacionUiState()
    )

    init {
        if (iniciarTicker) {
            viewModelScope.launch {
                while (isActive) {
                    delay(60_000)
                    tick.value = reloj.ahoraMs()
                }
            }
        }
        if (gestorPermisos != null) {
            viewModelScope.launch {
                _permisoState.value = gestorPermisos.estado(Permiso.NOTIFICACIONES)
            }
        }
    }

    fun avanzarReloj(ahora: Long) {
        tick.value = ahora
    }

    fun sincronizar() {
        viewModelScope.launch {
            coordinador.sincronizar(MotivoSync.MANUAL)
        }
    }

    fun reintentarDescarga() {
        viewModelScope.launch {
            coordinador.reintentarDescarga()
        }
    }

    fun revisar(id: Long, decision: DecisionEnvio, aceptaRiesgo: Boolean = false) {
        viewModelScope.launch {
            revisarEnvio(id, decision, aceptaRiesgo)
        }
    }

    fun solicitarPermisoAvisos() {
        viewModelScope.launch {
            val res = gestorPermisos?.solicitar(Permiso.NOTIFICACIONES) ?: EstadoPermiso.Denegado
            _permisoState.value = res
        }
    }

    fun descartarAvisoPermiso() {
        _permisoDescartado.value = true
    }

    fun abrirAjustes() {
        gestorPermisos?.abrirAjustes()
    }
}
