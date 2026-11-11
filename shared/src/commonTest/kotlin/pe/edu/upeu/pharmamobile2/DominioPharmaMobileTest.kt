package pe.edu.upeu.pharmamobile2

import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import kotlin.test.Test
import kotlin.test.assertEquals

class DominioPharmaMobileTest {

    // 1. Paso 1: Cliente y null-safety
    @Test
    fun testClienteNullSafety() {
        val clienteConTel = Cliente(
            id = 1L,
            nombre = "Ana Torres",
            correo = "ana@correo.com",
            telefono = "999888777"
        )
        val clienteSinTel = Cliente(
            id = 2L,
            nombre = "Luis Ramos",
            correo = "luis@correo.com",
            telefono = null
        )

        assertEquals("999888777", clienteConTel.obtenerTelefono())
        assertEquals("No registrado", clienteSinTel.obtenerTelefono())
    }

    // 2. Paso 2: Producto, inmutabilidad y copy()
    @Test
    fun testProductoInmutabilidadYCopy() {
        val original = Producto(id = 1, nombre = "Paracetamol", precio = 5.0, stock = 50)
        val actualizado = original.copy(stock = 90)

        assertEquals(50, original.stock)
        assertEquals(90, actualizado.stock)
        assertEquals("Paracetamol", actualizado.nombre)

        val disminuido = original.disminuirStock(10)
        assertEquals(40, disminuido.stock)
        assertEquals(50, original.stock)
    }

}
