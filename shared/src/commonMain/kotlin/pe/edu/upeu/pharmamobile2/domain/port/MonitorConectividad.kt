package pe.edu.upeu.pharmamobile2.domain.port

import kotlinx.coroutines.flow.StateFlow

enum class EstadoConectividad { DESCONOCIDA, DISPONIBLE, NO_DISPONIBLE }
interface MonitorConectividad {
    val estado: StateFlow<EstadoConectividad>
    fun disponible(): Boolean
    fun iniciar()
    fun detener()
}
