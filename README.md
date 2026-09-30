This is a Kotlin Multiplatform project targeting Android, iOS.

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./shared/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./shared/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./shared/src/jvmMain/kotlin)
    folder is the appropriate location.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :shared:testAndroidHostTest`
- iOS tests: `./gradlew :shared:iosSimulatorArm64Test`

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

---

# Integración REST con Ktor

## Rama de trabajo
`feature/ktor-client`

## URL base
- Android (Emulador): `http://10.0.2.2:8080/`
- iOS (Simulador): `http://localhost:8080/`

## Endpoint implementado
`GET /api/v1/productos?pagina=0&tamanio=20&ordenarPor=id&direccion=asc`

## Arquitectura
`UI (ProductoScreen) → ViewModel (ProductoViewModel) → Repository (ProductoRepositoryImpl) → ApiService (ProductoApi) → Ktor Client (HttpClient)`

## DTO
- `PaginaProductosDto`: Envoltorio de paginación (`contenido`, `pagina`, `totalElementos`, `totalPaginas`).
- `ProductoDto`: Mapeo directo de campos JSON (`id`, `nombre`, `precio`, `stock`, `estado`, `categoriaNombre`, etc.).

## Modelo de dominio
`Producto`: Entidad pura del negocio con reglas invariantes (`requiereReposicion`, `disminuirStock`).

## Motores por plataforma
- **Android:** OkHttp (`io.ktor:ktor-client-okhttp`)
- **iOS:** Darwin (`io.ktor:ktor-client-darwin`)

## Pruebas realizadas
- Respuesta 200 (Success): Carga de productos paginados y mapeo a dominio.
- Error 404 (Recurso no encontrado): Manejo seguro mediante `ClientRequestException` en `ProductoViewModel`.
- Sin conexión (Offline): Captura de `IOException` / fallo de red manteniendo la UI estable sin crashes.
- Timeout: Configurado con `HttpTimeout` (15s request / 10s connect).
- Campo desconocido en JSON: Verificado con `ignoreUnknownKeys = true` y `isLenient = true`.