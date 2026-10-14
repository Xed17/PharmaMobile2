package pe.edu.upeu.pharmamobile2.data.local

interface AlmacenSeguro {
    suspend fun guardarToken(token: String)
    suspend fun obtenerToken(): String?
    suspend fun eliminarToken()
}
