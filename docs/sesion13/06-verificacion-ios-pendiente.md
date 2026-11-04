# Verificación iOS — Sesión 13 (Pendiente de Ejecución en Mac)

**Entorno de desarrollo actual:** Windows 11 (Host de compilación KMP Android/JVM).  
**Estado general:** Adaptadores y contratos de arquitectura implementados en Kotlin Multiplatform (`shared/src/iosMain`), listos para compilar y validar en hardware o agente macOS con Xcode.  
**Criterio de honestidad técnica:** Ninguna prueba o evidencia de iOS se simula ni se atribuye como ejecutada en Windows. Todo queda registrado como `PREPARADO / NO EJECUTADO EN MAC`.

---

## 1. Inventario de Adaptadores iOS Implementados

| Contrato Común (`commonMain`) | Implementación iOS (`iosMain`) | Estado en Windows | Validación Pendiente en macOS |
| --- | --- | --- | --- |
| `GestorPermisos` | `pe.edu.upeu.pharmamobile2.data.permiso.GestorPermisosIos` | PREPARADO / NO EJECUTADO EN MAC | `UNUserNotificationCenter.current().requestAuthorizationWithOptions` |
| `NotificadorSincronizacion` | `pe.edu.upeu.pharmamobile2.data.notificacion.NotificadorIos` | PREPARADO / NO EJECUTADO EN MAC | `UNNotificationRequest` con identificador de cuenta |
| `MonitorConectividad` | `pe.edu.upeu.pharmamobile2.platform.MonitorConectividad` | PREPARADO / NO EJECUTADO EN MAC | `NWPathMonitor` sobre framework Network |
| `Telemetria` | `pe.edu.upeu.pharmamobile2.domain.port.Telemetria` | PREPARADO / NO EJECUTADO EN MAC | Firebase Crashlytics vía SPM (`https://github.com/firebase/firebase-ios-sdk`) |

---

## 2. Comandos Requeridos para Ejecución en macOS

Para un desarrollador o pipeline con acceso a macOS y Xcode:

### Paso 1: Pruebas unitarias de Kotlin Multiplatform en simulador iOS
```bash
./gradlew :shared:iosSimulatorArm64Test --console=plain
```
*Evidencia esperada:* Reporte JUnit en `shared/build/reports/tests/iosSimulatorArm64Test/index.html` con todos los tests de dominio, reintentos y política de avisos pasando.

### Paso 2: Listar esquemas y simuladores disponibles
```bash
xcodebuild -list -project iosApp/iosApp.xcodeproj
xcrun simctl list devices available
```

### Paso 3: Compilación de la aplicación iOS sin firma (Simulator Build)
```bash
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'platform=iOS Simulator,name=iPhone 16' \
  CODE_SIGNING_ALLOWED=NO \
  build
```
*Evidencia esperada:* `** BUILD SUCCEEDED **`

### Paso 4: Pruebas de permisos y avisos en primer plano
1. Lanzar la app en el simulador.
2. Comprobar diálogo nativo de `UserNotifications` al pulsar «Activar».
3. Validar los tres estados: Concedido, Denegado y DenegadoParaSiempre (redirección a `UIApplicationOpenSettingsURLString`).

---

## 3. Matriz de Estado de Tareas iOS

| Tarea | Requisito | Estado | Justificación |
| --- | --- | --- | --- |
| T14.1 | Adaptadores de permisos y avisos | CUMPLIDO (Código) | Contratos comunes implementados y registrados en `PlatformModule.ios.kt`. |
| T14.2 | Ejecución de tests en simulador iOS | PENDIENTE EN MAC | Requiere daemon y SDK de macOS/Xcode. |
| T14.3 | Compilación de `iosApp.xcodeproj` | PENDIENTE EN MAC | Requiere macOS con SPM y toolchain Swift/Clang. |
| T14.4 | Capturas de pantalla iOS | PENDIENTE EN MAC | No se fabrican ni emulan en Windows. |
