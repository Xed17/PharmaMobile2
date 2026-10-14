package pe.edu.upeu.pharmamobile2

import pe.edu.upeu.pharmamobile2.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobile2.data.remote.dto.UsuarioActualDto
import pe.edu.upeu.pharmamobile2.data.remote.mapper.toDomain
import pe.edu.upeu.pharmamobile2.domain.model.Sesion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermisosTest {

    @Test
    fun testSesionPuede_permisoOtorgado_retornaTrue() {
        val sesion = Sesion(
            token = "jwt.test.token",
            username = "admin",
            nombreCompleto = "Administrador Sistema",
            roles = listOf("ADMIN"),
            permisos = setOf("CATALOGO_ESCRIBIR", "CATALOGO_LEER", "CLIENTES_LEER")
        )

        assertTrue(sesion.puede("CATALOGO_ESCRIBIR"))
        assertTrue(sesion.puede("CATALOGO_LEER"))
        assertTrue(sesion.puede("CLIENTES_LEER"))
    }

    @Test
    fun testSesionPuede_permisoNoOtorgado_retornaFalse() {
        // Rol vendedor no tiene CATALOGO_ESCRIBIR
        val sesionVendedor = Sesion(
            token = "jwt.test.token",
            username = "vendedor",
            nombreCompleto = "Vendedor Test",
            roles = listOf("VENDEDOR"),
            permisos = setOf("CATALOGO_LEER", "CLIENTES_LEER", "VENTAS_REGISTRAR")
        )

        assertFalse(sesionVendedor.puede("CATALOGO_ESCRIBIR"))
        assertFalse(sesionVendedor.puede("REPORTES_VER"))

        // Rol almacen no tiene CLIENTES_LEER
        val sesionAlmacen = Sesion(
            token = "jwt.test.token",
            username = "almacen",
            nombreCompleto = "Almacenero Test",
            roles = listOf("ALMACEN"),
            permisos = setOf("CATALOGO_ESCRIBIR", "CATALOGO_LEER")
        )

        assertFalse(sesionAlmacen.puede("CLIENTES_LEER"))
        assertFalse(sesionAlmacen.puede("VENTAS_REGISTRAR"))
    }

    @Test
    fun testMapperLoginResponseDtoToDomain() {
        val dto = LoginResponseDto(
            token = "jwt.token.login",
            tipo = "Bearer",
            expiraEnSegundos = 3600L,
            username = "admin",
            nombreCompleto = "Administrador General",
            roles = listOf("ROLE_ADMIN"),
            permisos = listOf("CATALOGO_LEER", "CATALOGO_ESCRIBIR")
        )

        val sesion = dto.toDomain()
        assertEquals("jwt.token.login", sesion.token)
        assertEquals("Bearer", sesion.tipo)
        assertEquals(3600L, sesion.expiraEnSegundos)
        assertEquals("admin", sesion.username)
        assertEquals("Administrador General", sesion.nombreCompleto)
        assertEquals(listOf("ROLE_ADMIN"), sesion.roles)
        assertEquals(setOf("CATALOGO_LEER", "CATALOGO_ESCRIBIR"), sesion.permisos)
        assertTrue(sesion.puede("CATALOGO_LEER"))
    }

    @Test
    fun testMapperUsuarioActualDtoToDomain() {
        val dto = UsuarioActualDto(
            id = 10L,
            username = "almacen",
            empleadoId = 5L,
            empleadoNombre = "Carlos Almacén",
            cargo = "Encargado de Almacén",
            roles = listOf("ROLE_ALMACEN"),
            permisos = listOf("CATALOGO_ESCRIBIR", "CATALOGO_LEER"),
            estado = true
        )

        val sesion = dto.toDomain(token = "jwt.stored.token")
        assertEquals("jwt.stored.token", sesion.token)
        assertEquals("almacen", sesion.username)
        assertEquals("Carlos Almacén", sesion.nombreCompleto)
        assertTrue(sesion.puede("CATALOGO_ESCRIBIR"))
        assertFalse(sesion.puede("CLIENTES_LEER"))
    }
}
