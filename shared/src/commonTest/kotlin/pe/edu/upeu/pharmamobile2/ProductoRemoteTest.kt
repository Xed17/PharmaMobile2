package pe.edu.upeu.pharmamobile2

import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile2.data.mapper.toDomain
import pe.edu.upeu.pharmamobile2.data.remote.dto.PaginaProductosDto
import pe.edu.upeu.pharmamobile2.data.remote.dto.ProductoDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductoRemoteTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Test
    fun testDeserializacionPaginaYCamposDesconocidos() {
        val jsonString = """
        {
          "contenido": [
            {
              "id": 1,
              "nombre": "Paracetamol 500mg",
              "precio": 2.50,
              "stock": 100,
              "estado": true,
              "categoriaId": 3,
              "categoriaNombre": "Analgésicos",
              "fechaCreacion": "2026-09-30T10:15:30",
              "fechaModificacion": "2026-09-30T10:15:30",
              "campoExtraInesperado": "valor ignorado"
            }
          ],
          "pagina": 0,
          "tamanio": 10,
          "totalElementos": 1,
          "totalPaginas": 1,
          "ultima": true,
          "metadataBackend": 12345
        }
        """.trimIndent()

        val pagina = json.decodeFromString<PaginaProductosDto>(jsonString)
        assertEquals(1, pagina.contenido.size)
        assertEquals(0, pagina.pagina)
        assertTrue(pagina.ultima)

        val dto = pagina.contenido.first()
        assertEquals(1, dto.id)
        assertEquals("Paracetamol 500mg", dto.nombre)
        assertEquals(2.50, dto.precio)
        assertEquals(100, dto.stock)
        assertTrue(dto.estado)

        // Verificar el mapper
        val dominio = dto.toDomain()
        assertEquals(1, dominio.id)
        assertEquals("Paracetamol 500mg", dominio.nombre)
        assertEquals(2.50, dominio.precio)
        assertEquals(100, dominio.stock)
        assertTrue(dominio.activo)
        assertTrue(dominio.verificarStock(50))
    }

    @Test
    fun testMapperProductoSinStock() {
        val dto = ProductoDto(
            id = 2,
            nombre = "Ibuprofeno 400mg",
            precio = 5.0,
            stock = 0,
            estado = false
        )
        val dominio = dto.toDomain()
        assertEquals(2, dominio.id)
        assertEquals(0, dominio.stock)
        assertTrue(!dominio.activo)
        assertTrue(!dominio.estadoDisponible())
        assertTrue(dominio.requiereReposicion())
    }
}
