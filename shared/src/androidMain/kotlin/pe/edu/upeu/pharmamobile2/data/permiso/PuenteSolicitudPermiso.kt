package pe.edu.upeu.pharmamobile2.data.permiso

enum class ResultadoSolicitudAndroid {
    Concedido,
    Rechazado,
    Cancelado
}

interface PuenteSolicitudPermiso {
    suspend fun solicitar(permiso: String): ResultadoSolicitudAndroid
    fun debeMostrarJustificacion(permiso: String): Boolean
    fun abrirAjustes()
}

object PuentePermisosHolder : PuenteSolicitudPermiso {
    @Volatile
    private var delegado: PuenteSolicitudPermiso? = null

    fun vincular(puente: PuenteSolicitudPermiso) {
        delegado = puente
    }

    fun desvincular() {
        delegado = null
    }

    override suspend fun solicitar(permiso: String): ResultadoSolicitudAndroid {
        return delegado?.solicitar(permiso) ?: ResultadoSolicitudAndroid.Cancelado
    }

    override fun debeMostrarJustificacion(permiso: String): Boolean {
        return delegado?.debeMostrarJustificacion(permiso) ?: false
    }

    override fun abrirAjustes() {
        delegado?.abrirAjustes()
    }
}
