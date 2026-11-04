package pe.edu.upeu.pharmamobile2

import kotlin.test.*
import pe.edu.upeu.pharmamobile2.domain.sync.PoliticaReintento
import pe.edu.upeu.pharmamobile2.domain.model.*

class PoliticaReintentoTest {
    private val politica = PoliticaReintento()
    @Test fun esperaCreceYSeAcotaSinOverflow() {
        assertEquals(listOf(1000L, 2000L, 4000L, 8000L, 16000L), (0..4).map(politica::esperaMs))
        assertEquals(16000L, politica.esperaMs(Int.MAX_VALUE))
        assertFailsWith<IllegalArgumentException> { politica.esperaMs(-1) }
    }
    @Test fun resultadoInciertoNuncaHabilitaReenvioAutomatico() {
        val error = FalloOperacion(NaturalezaFallo.TRANSITORIO, "TIMEOUT", "Tiempo agotado", efectoIncierto = true)
        assertFalse(politica.puedeReintentarAutomaticamente(error, esLectura = false))
        assertFalse(politica.puedeReintentarAutomaticamente(error, esLectura = true))
    }
    @Test fun lecturaTransitoriaPermiteReintentoPeroRechazosNo() {
        val temporal = FalloOperacion(NaturalezaFallo.TRANSITORIO, "SIN_RED", "Sin red")
        assertTrue(politica.puedeReintentarAutomaticamente(temporal, esLectura = true))
        assertFalse(politica.puedeReintentarAutomaticamente(temporal, esLectura = false))
        for (tipo in listOf(NaturalezaFallo.PERMANENTE, NaturalezaFallo.SESION, NaturalezaFallo.PERMISOS)) {
            assertFalse(politica.puedeReintentarAutomaticamente(temporal.copy(naturaleza = tipo), esLectura = true))
        }
    }
    @Test fun soloPendientesConPresupuestoPuedenIniciarEnvio() {
        assertTrue(politica.puedeIniciarEnvio(EstadoEnvio.PENDIENTE, 0))
        assertTrue(politica.puedeIniciarEnvio(EstadoEnvio.PENDIENTE, 4))
        assertFalse(politica.puedeIniciarEnvio(EstadoEnvio.PENDIENTE, 5))
        for (estado in EstadoEnvio.entries.filter { it != EstadoEnvio.PENDIENTE }) {
            assertFalse(politica.puedeIniciarEnvio(estado, 0))
        }
        assertFailsWith<IllegalArgumentException> { politica.puedeIniciarEnvio(EstadoEnvio.PENDIENTE, -1) }
    }
}
