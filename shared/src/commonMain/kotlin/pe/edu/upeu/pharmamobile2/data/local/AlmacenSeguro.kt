package pe.edu.upeu.pharmamobile2.data.local

import pe.edu.upeu.pharmamobile2.domain.model.PerfilLocal

interface AlmacenSeguro {
    suspend fun guardarToken(token: String)
    suspend fun obtenerToken(): String?
    suspend fun eliminarToken()
    suspend fun guardarPerfil(perfil: PerfilLocal)
    suspend fun obtenerPerfil(): PerfilLocal?
    suspend fun borrarContinuidad()
}
