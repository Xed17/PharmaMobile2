package pe.edu.upeu.pharmamobile2.data.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics
import pe.edu.upeu.pharmamobile2.domain.port.Telemetria

class TelemetriaFirebase : Telemetria {

    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            contexto.forEach { (clave, valor) ->
                crashlytics.setCustomKey(clave, valor)
            }
            crashlytics.recordException(e)
        } catch (_: Throwable) {
            // Firebase no inicializado o fuera de dispositivo
        }
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            val datosStr = if (datos.isNotEmpty()) {
                datos.entries.joinToString(prefix = " {", postfix = "}") { "${it.key}=${it.value}" }
            } else ""
            crashlytics.log("[$nombre]$datosStr")
        } catch (_: Throwable) {
            // Firebase no inicializado
        }
    }
}
