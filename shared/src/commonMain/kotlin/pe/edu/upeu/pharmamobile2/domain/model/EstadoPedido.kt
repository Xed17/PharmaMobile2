package pe.edu.upeu.pharmamobile2.domain.model

sealed class EstadoPedido {
    data object Pendiente: EstadoPedido()
    data object Procesando: EstadoPedido()
    data object Entregado: EstadoPedido()
    data class Rechazado(val motivo: String) : EstadoPedido()
}

fun mensajeEstado(estado: EstadoPedido): String = when (estado) {
    EstadoPedido.Pendiente -> "El pedido está pendiente"
    EstadoPedido.Procesando -> "El pedido se está procesando"
    EstadoPedido.Entregado -> "El pedido ha sido entregado"
    is EstadoPedido.Rechazado -> "Pedido rechazado: ${estado.motivo}"
}