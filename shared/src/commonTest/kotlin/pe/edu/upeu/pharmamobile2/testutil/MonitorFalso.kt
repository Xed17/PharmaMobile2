package pe.edu.upeu.pharmamobile2.testutil

import kotlinx.coroutines.flow.MutableStateFlow
import pe.edu.upeu.pharmamobile2.domain.port.*

class MonitorFalso(inicial: EstadoConectividad = EstadoConectividad.DISPONIBLE) : MonitorConectividad {
    override val estado = MutableStateFlow(inicial)
    override fun disponible() = estado.value == EstadoConectividad.DISPONIBLE
    override fun iniciar() {}
    override fun detener() { estado.value = EstadoConectividad.DESCONOCIDA }
}
