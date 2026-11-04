package pe.edu.upeu.pharmamobile2.data.firebase

import pe.edu.upeu.pharmamobile2.domain.port.Telemetria

class TelemetriaIos : Telemetria {
    override fun registrarError(e: Throwable, contexto: Map<String, String>) {}
    override fun registrarEvento(nombre: String, datos: Map<String, String>) {}
}
