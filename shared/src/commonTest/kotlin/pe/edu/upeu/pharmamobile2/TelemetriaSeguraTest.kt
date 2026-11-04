package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile2.data.firebase.TelemetriaSegura
import pe.edu.upeu.pharmamobile2.domain.port.Telemetria
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelemetriaSeguraTest {

    private class ProveedorEspia : Telemetria {
        val errores = mutableListOf<Pair<Throwable, Map<String, String>>>()
        val eventos = mutableListOf<Pair<String, Map<String, String>>>()
        var debeLanzarExcepcion = false

        override fun registrarError(e: Throwable, contexto: Map<String, String>) {
            if (debeLanzarExcepcion) throw RuntimeException("Crashlytics no disponible")
            errores.add(Pair(e, contexto))
        }

        override fun registrarEvento(nombre: String, datos: Map<String, String>) {
            if (debeLanzarExcepcion) throw RuntimeException("Crashlytics no disponible")
            eventos.add(Pair(nombre, datos))
        }
    }

    @Test
    fun mensajeYCausaNoExponenToken() {
        val proveedor = ProveedorEspia()
        val telemetria = TelemetriaSegura(proveedor)

        val causa = RuntimeException("Fallo al autenticar persona@correo.test con Bearer secreto123")
        val error = RuntimeException("Error guardando Producto privado XYZ", causa)

        telemetria.registrarError(error, mapOf("pantalla" to "PRODUCTOS", "operacion" to "SYNC_SUBIDA"))

        val salidaProveedor = buildString {
            proveedor.errores.forEach { (err, ctx) ->
                append(err.message ?: "")
                append(err.cause?.message ?: "")
                ctx.forEach { (k, v) -> append("$k=$v ") }
            }
        }

        assertFalse(salidaProveedor.contains("Bearer secreto"))
        assertFalse(salidaProveedor.contains("persona@correo.test"))
        assertFalse(salidaProveedor.contains("Producto privado"))
    }

    @Test
    fun contextoPermitidoNoAceptaDatosLibres() {
        val proveedor = ProveedorEspia()
        val telemetria = TelemetriaSegura(proveedor)

        telemetria.registrarError(
            RuntimeException("Error"),
            mapOf(
                "pantalla" to "PRODUCTOS",
                "operacion" to "SYNC_SUBIDA",
                "campo_inseguro" to "datos_privados",
                "cantidad" to "5",
                "cantidad_invalida" to "-1",
                "estado" to "FALLO"
            )
        )

        val ctx = proveedor.errores.first().second
        assertEquals("PRODUCTOS", ctx["pantalla"])
        assertEquals("SYNC_SUBIDA", ctx["operacion"])
        assertEquals("5", ctx["cantidad"])
        assertEquals("FALLO", ctx["estado"])
        assertFalse(ctx.containsKey("campo_inseguro"))
        assertFalse(ctx.containsKey("cantidad_invalida"))
    }

    @Test
    fun cancelacionNoSeReportaComoCrash() {
        val proveedor = ProveedorEspia()
        val telemetria = TelemetriaSegura(proveedor)

        telemetria.registrarError(CancellationException("Scope cancelado por el usuario"))
        assertEquals(0, proveedor.errores.size)
    }

    @Test
    fun proveedorRotoNoRompeGuardado() {
        val proveedor = ProveedorEspia()
        proveedor.debeLanzarExcepcion = true
        val telemetria = TelemetriaSegura(proveedor)

        val guardadoConProveedorQueFalla = runCatching {
            telemetria.registrarError(RuntimeException("Algo falló"))
            telemetria.registrarEvento("evento_prueba")
        }

        assertTrue(guardadoConProveedorQueFalla.isSuccess)
    }
}
