package pe.edu.upeu.pharmamobile2.platform

import platform.UIKit.UIDevice

actual class InfoDispositivo actual constructor() {
    actual val sistema: String = UIDevice.currentDevice.systemName
    actual val version: String = UIDevice.currentDevice.systemVersion
}
