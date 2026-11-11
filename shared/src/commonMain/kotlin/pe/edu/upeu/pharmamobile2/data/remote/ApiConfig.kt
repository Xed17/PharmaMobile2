package pe.edu.upeu.pharmamobile2.data.remote

/** URLs base del backend de desarrollo (en release debe proveerlas la plataforma por HTTPS). */
object ApiConfig {
    /** El emulador Android ve el host de la maquina como 10.0.2.2. */
    const val URL_EMULADOR_ANDROID = "http://10.0.2.2:8080/"
    const val URL_SIMULADOR_IOS = "http://localhost:8080/"
}
