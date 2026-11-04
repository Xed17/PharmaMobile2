# Backlog de Tareas — Sesión 14 y Cierre de Unidad

**Objetivo:** Planificar las tareas que suceden a la Sesión 13, integrando los módulos restantes de negocio contra el backend PharmaSoft y completando la verificación en dispositivos físicos y ecosistema Apple.

---

## 1. Módulos de Negocio Pendientes de Integración REST

### T14-01: Integración del Módulo de Clientes con Backend PharmaSoft
- **Contexto:** En la Sesión 13, el módulo de Clientes opera como demostración local en memoria (`InMemoryRepository`).
- **Alcance Sesión 14:**
  - Implementar `ClienteApi` con Ktor para consumir endpoints REST `/api/v1/clientes`.
  - Crear tabla local SQLite `cliente (cuentaClave TEXT, id INTEGER, ...)` con migración SQLDelight.
  - Implementar `ClienteRepositoryOffline` con soporte de búsqueda local por DNI y sincronización en segundo plano.
  - Añadir suite de pruebas en `commonTest`.

### T14-02: Integración del Módulo de Pedidos y Ventas
- **Contexto:** En la Sesión 13, el módulo de Pedidos permite registrar órdenes usando catálogo simulado.
- **Alcance Sesión 14:**
  - Vincular la creación de pedidos con el catálogo real sincronizado en SQLite.
  - Implementar descuento transaccional preventivo de stock local al generar pedido offline.
  - Conectar con el endpoint `/api/v1/ventas` de PharmaSoft con soporte de reintento seguro y validación de cliente.

---

## 2. Validación de Plataforma iOS en macOS

### T14-03: Ejecución de la Suite KMP en Simulador iOS
- **Entorno requerido:** Estación macOS con Xcode 16+ y CocoaPods / Swift Package Manager.
- **Tareas:**
  - Ejecutar `./gradlew :shared:iosSimulatorArm64Test`.
  - Validar paso de tests en simulador de iPhone 16.
  - Compilar `iosApp.xcodeproj` mediante `xcodebuild`.
  - Capturar evidencias nativas de solicitud de permiso de notificaciones con `UNUserNotificationCenter`.

---

## 3. Cierre de Unidad y Despliegue en Dispositivos Físicos

### T14-04: Generación de Builds Firmados (Release)
- **Android:**
  - Configurar `signingConfigs.release` en `androidApp/build.gradle.kts` con Keystore local.
  - Generar APK / Android App Bundle (`.aab`) optimizado con R8 / ProGuard.
- **iOS:**
  - Configurar certificados de desarrollo / ad-hoc en Apple Developer Program.
  - Exportar `.ipa` firmado.

### T14-05: Sustentación y Pruebas en Dispositivos Físicos
- Instalación en smartphone Android físico (modo avión físico, desconexión de antena, reconexión móvil).
- Validación de latencia y comportamiento de notificaciones locales ante reinicio del equipo.
