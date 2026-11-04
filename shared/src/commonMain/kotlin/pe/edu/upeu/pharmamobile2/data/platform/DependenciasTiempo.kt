package pe.edu.upeu.pharmamobile2.data.platform

import kotlinx.coroutines.delay
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import pe.edu.upeu.pharmamobile2.domain.port.*
import pe.edu.upeu.pharmamobile2.platform.obtenerTiempoActualMs

class RelojSistema : Reloj { override fun ahoraMs() = obtenerTiempoActualMs() }
class EsperaCoroutines : Espera { override suspend fun esperar(ms: Long) { delay(ms) } }
@OptIn(ExperimentalUuidApi::class)
class GeneradorUuid : GeneradorIds { override fun nuevo() = Uuid.random().toString() }
