package pe.edu.upeu.pharmamobile2.presentation.sincronizacion

import pe.edu.upeu.pharmamobile2.domain.port.EstadoPermiso

data class SincronizacionUiState(
    val enCurso: Boolean = false,
    val pendientes: Int = 0,
    val fallidas: Int = 0,
    val revision: Int = 0,
    val bloqueadas: Int = 0,
    val ultimaSync: Long? = null,
    val antiguedad: String = "Nunca sincronizado",
    val mensaje: String? = null,
    val puedeReintentarDescarga: Boolean = false,
    val estadoPermisoAvisos: EstadoPermiso = EstadoPermiso.Denegado,
    val avisoPermisoDescartado: Boolean = false
)
