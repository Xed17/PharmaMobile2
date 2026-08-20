package pe.edu.upeu.pharmamobile2.domain.model

data class Pedido (
    val id: Int,
    val cliente: Cliente,
    val detalles: List<DetallePedido>,
    val estado: EstadoPedido
)