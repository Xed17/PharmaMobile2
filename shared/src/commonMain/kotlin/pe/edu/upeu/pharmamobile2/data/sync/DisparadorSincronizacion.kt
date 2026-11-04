package pe.edu.upeu.pharmamobile2.data.sync

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import pe.edu.upeu.pharmamobile2.domain.model.MotivoSync

/** A wake-up signal only: durable pending rows remain in SQLite. */
class DisparadorSincronizacion {
    private val solicitudes = Channel<MotivoSync>(Channel.CONFLATED)
    val eventos = solicitudes.receiveAsFlow()
    fun solicitar(motivo: MotivoSync) { solicitudes.trySend(motivo) }
    fun cerrar() { solicitudes.close() }
}
