package pe.edu.upeu.pharmamobile2.data

import pe.edu.upeu.pharmamobile2.domain.port.LimpiadorDatosSesion

class LimpiadorDatosDemo : LimpiadorDatosSesion {
    override fun limpiar() = InMemoryRepository.limpiarDatosDeSesion()
}
