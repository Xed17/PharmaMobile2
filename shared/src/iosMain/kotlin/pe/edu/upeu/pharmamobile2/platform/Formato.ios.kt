package pe.edu.upeu.pharmamobile2.platform

import platform.Foundation.NSLocale
import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterCurrencyStyle

actual fun formatearSoles(valor: Double): String {
    val formateador = NSNumberFormatter()

    formateador.numberStyle = NSNumberFormatterCurrencyStyle
    formateador.locale = NSLocale(localeIdentifier = "es_PE")

    return formateador.stringFromNumber(NSNumber(valor))
        ?: "S/ $valor"
}
