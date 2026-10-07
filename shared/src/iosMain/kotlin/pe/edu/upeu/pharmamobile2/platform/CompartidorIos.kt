package pe.edu.upeu.pharmamobile2.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import pe.edu.upeu.pharmamobile2.domain.platform.Compartidor

class CompartidorIos : Compartidor {

    @OptIn(ExperimentalForeignApi::class)
    override fun compartir(texto: String) {
        val controlador = UIActivityViewController(
            activityItems = listOf(texto),
            applicationActivities = null
        )

        val ventana = UIApplication.sharedApplication.keyWindow
        val controladorRaiz = ventana?.rootViewController

        controladorRaiz?.presentViewController(
            controlador,
            animated = true,
            completion = null
        )
    }
}
