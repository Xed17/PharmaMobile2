package pe.edu.upeu.pharmamobile2.platform

import java.text.NumberFormat
import java.util.Locale

actual fun formatearSoles(valor: Double): String {
    return NumberFormat
        .getCurrencyInstance(Locale.forLanguageTag("es-PE"))
        .format(valor)
}
