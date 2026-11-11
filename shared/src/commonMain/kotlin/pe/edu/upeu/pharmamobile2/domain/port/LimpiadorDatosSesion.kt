package pe.edu.upeu.pharmamobile2.domain.port

/** Descarta los datos en memoria de la cuenta al cerrar sesion. */
fun interface LimpiadorDatosSesion {
    fun limpiar()
}
