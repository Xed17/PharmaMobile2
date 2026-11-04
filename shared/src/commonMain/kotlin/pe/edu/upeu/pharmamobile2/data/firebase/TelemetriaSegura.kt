package pe.edu.upeu.pharmamobile2.data.firebase

import kotlinx.coroutines.CancellationException
import pe.edu.upeu.pharmamobile2.domain.port.Telemetria

class ExcepcionTecnicaSanitizada(
    val tipoOriginal: String,
    mensajeFijo: String = "Error técnico controlado"
) : Exception("[$tipoOriginal] $mensajeFijo")

class TelemetriaSegura(
    private val delegate: Telemetria
) : Telemetria {

    companion object {
        private val CLAVES_PERMITIDAS = setOf("pantalla", "operacion", "tipo_error", "estado", "cantidad")

        private val PANTALLAS_PERMITIDAS = setOf(
            "INICIO", "PRODUCTOS", "DETALLE", "CLIENTES", "PEDIDOS", "REVISION_ENVIOS", "LOGIN", "ACERCA_DE"
        )
        private val OPERACIONES_PERMITIDAS = setOf(
            "SYNC_DESCARGA", "SYNC_SUBIDA", "REVISAR_ENVIO", "LOGIN", "CREAR_PRODUCTO", "ACTUALIZAR_PRODUCTO", "ELIMINAR_PRODUCTO", "CONSULTA"
        )
        private val TIPOS_ERROR_PERMITIDOS = setOf(
            "RED", "SESION", "CONCURRENCIA", "LOCAL", "VALIDACION", "DESCONOCIDO", "TRANSITORIO", "PERMANENTE"
        )
        private val ESTADOS_PERMITIDOS = setOf(
            "INICIADO", "EXITO", "FALLO", "REINTENTO", "CANCELADO", "AGOTADO"
        )

        fun validarYFiltrar(datos: Map<String, String>): Map<String, String> {
            val filtrados = mutableMapOf<String, String>()
            for ((clave, valor) in datos) {
                if (clave !in CLAVES_PERMITIDAS) continue

                val esValido = when (clave) {
                    "pantalla" -> valor.uppercase() in PANTALLAS_PERMITIDAS
                    "operacion" -> valor.uppercase() in OPERACIONES_PERMITIDAS
                    "tipo_error" -> valor.uppercase() in TIPOS_ERROR_PERMITIDOS
                    "estado" -> valor.uppercase() in ESTADOS_PERMITIDOS
                    "cantidad" -> valor.toIntOrNull()?.let { it >= 0 } == true
                    else -> false
                }
                if (esValido) {
                    filtrados[clave] = valor
                }
            }
            return filtrados
        }
    }

    override fun registrarError(e: Throwable, contexto: Map<String, String>) {
        if (e is CancellationException) return

        try {
            val ctxFiltrado = validarYFiltrar(contexto)
            val tipoOriginal = e::class.simpleName ?: "Throwable"
            val excepcionSanitizada = ExcepcionTecnicaSanitizada(tipoOriginal)
            delegate.registrarError(excepcionSanitizada, ctxFiltrado)
        } catch (_: Throwable) {
            // Un fallo en el proveedor de telemetría nunca debe romper el flujo de la aplicación
        }
    }

    override fun registrarEvento(nombre: String, datos: Map<String, String>) {
        try {
            val datosFiltrados = validarYFiltrar(datos)
            delegate.registrarEvento(nombre, datosFiltrados)
        } catch (_: Throwable) {
            // Protección contra fallos del proveedor
        }
    }
}
