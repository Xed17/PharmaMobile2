package pe.edu.upeu.pharmamobile2

import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicAndroidHostTest {

    @Test
    fun clienteTelefono() {
        val cliente = Cliente(
            id = 1L,
            nombre = "Farmacia Central",
            correo = "ventas@central.pe",
            telefono = "987654321"
        )
        val resultado = cliente.obtenerTelefono()

        assertEquals("987654321", resultado)
    }
}