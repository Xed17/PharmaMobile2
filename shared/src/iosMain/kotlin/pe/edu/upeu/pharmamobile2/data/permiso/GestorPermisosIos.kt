package pe.edu.upeu.pharmamobile2.data.permiso

import pe.edu.upeu.pharmamobile2.domain.port.EstadoPermiso
import pe.edu.upeu.pharmamobile2.domain.port.GestorPermisos
import pe.edu.upeu.pharmamobile2.domain.port.Permiso

class GestorPermisosIos : GestorPermisos {
    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        return EstadoPermiso.Denegado
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        return EstadoPermiso.Denegado
    }

    override fun abrirAjustes() {
        // Preparado para abrir UIApplicationOpenSettingsURLString en Mac
    }
}
