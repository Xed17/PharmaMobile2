package pe.edu.upeu.pharmamobile2.testutil

import kotlinx.coroutines.delay
import pe.edu.upeu.pharmamobile2.domain.port.*

class RelojFalso(var instanteMs: Long = 0) : Reloj { override fun ahoraMs() = instanteMs }
class EsperaFalsa(val demoras: MutableList<Long> = mutableListOf()) : Espera {
    override suspend fun esperar(ms: Long) { demoras.add(ms); delay(ms) }
}
class GeneradorIdsSecuencial : GeneradorIds {
    private var siguiente = 0L
    override fun nuevo() = "operacion-prueba-${++siguiente}"
}
class TelemetriaFalsa : Telemetria {
    val errores = mutableListOf<Pair<Throwable, Map<String, String>>>()
    val eventos = mutableListOf<Pair<String, Map<String, String>>>()
    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        errores.add(Pair(e, contexto))
    }
    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        eventos.add(Pair(nombre, datos))
    }
}
