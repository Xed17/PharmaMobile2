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

# Conectividad REST (Integración con Ktor Client)

## Rama de trabajo
`feature/ktor-client`

## URL base
- Android (Emulador): `http://10.0.2.2:8080/`
- iOS (Simulador): `http://localhost:8080/`

## Endpoint implementado
`GET /api/v1/productos?pagina=0&tamanio=20&ordenarPor=id&direccion=asc`

## Arquitectura
`UI (ProductoScreen) → ViewModel (ProductoViewModel) → Repository (ProductoRepositoryImpl) → ApiService (ProductoApi) → Ktor Client (HttpClient)`

## DTO (Data Transfer Objects)

La capa remota desacopla la estructura JSON del servidor mediante dos DTOs serializables con `kotlinx.serialization`:

### 1. `PaginaProductosDto` (Estructura de Paginación de Spring Boot)
Envuelve la respuesta paginada devuelta por el backend:

| Campo | Tipo Kotlin | Tipo JSON | Requerido / Default | Descripción |
|---|---|---|---|---|
| `contenido` | `List<ProductoDto>` | `Array` | Default: `emptyList()` | Lista de productos correspondientes a la página consultada. |
| `pagina` | `Int` | `Number` | Default: `0` | Índice de la página actual (base 0). |
| `tamanio` | `Int` | `Number` | Default: `0` | Cantidad de elementos por página solicitados. |
| `totalElementos` | `Long` | `Number` | Default: `0` | Cantidad total de registros disponibles en la base de datos. |
| `totalPaginas` | `Int` | `Number` | Default: `0` | Total de páginas calculadas por el backend. |
| `ultima` | `Boolean` | `Boolean` | Default: `true` | Indica si es la última página disponible del conjunto de datos. |

### 2. `ProductoDto` (Entidad de Transferencia de Producto)
Representa el esquema exacto de un producto farmacéutico:

| Campo | Tipo Kotlin | Tipo JSON | Requerido / Default | Descripción |
|---|---|---|---|---|
| `id` | `Int` | `Number` | Requerido | Identificador único del producto en la base de datos. |
| `nombre` | `String` | `String` | Requerido | Nombre comercial o denominación del medicamento/producto. |
| `precio` | `Double` | `Number` | Requerido | Precio de venta unitario. |
| `stock` | `Int` | `Number` | Default: `0` | Unidades disponibles en el inventario. |
| `estado` | `Boolean` | `Boolean` | Default: `true` | Estado operativo del producto (`true`: activo, `false`: inactivo). |
| `categoriaId` | `Int?` | `Number / null` | Opcional (`null`) | Identificador único de la categoría asociada. |
| `categoriaNombre` | `String?` | `String / null` | Opcional (`null`) | Nombre legible de la categoría (ej. Analgésicos, Antibióticos). |
| `fechaCreacion` | `String?` | `String / null` | Opcional (`null`) | Marca de tiempo ISO-8601 de creación del registro. |
| `fechaModificacion` | `String?` | `String / null` | Opcional (`null`) | Marca de tiempo ISO-8601 de última actualización. |

## Mapeo a Dominio (Mapper)
La función de extensión `ProductoDto.toDomain(): Producto` transforma el DTO de infraestructura al modelo puro de negocio:
- `id` → `id: Long` (conversión segura a tipo numérico de dominio)
- `nombre` → `nombre: String`
- `precio` → `precio: Double`
- `stock` → `stock: Int`
- `estado` → `activo: Boolean` (adaptación semántica del estado)
- `categoriaNombre` → Manejado o asignado como metadata si el modelo lo requiere.

## Modelo de dominio
`Producto`: Entidad pura del negocio ubicada en `domain/model/Producto.kt`, completamente agnóstica a Ktor y serialización, con lógica de negocio e invariantes (`requiereReposicion`, `disminuirStock`).

## Motores por plataforma
- **Android:** OkHttp (`io.ktor:ktor-client-okhttp`) configurado en `PlatformModule.android.kt`.
- **iOS:** Darwin (`io.ktor:ktor-client-darwin`) configurado en `PlatformModule.ios.kt`.

## Pruebas realizadas
- **Respuesta 200 (Success):** Carga de productos paginados y mapeo a dominio verificado tanto en test de integración como en emulador.
- **Error 404 (Recurso no encontrado):** Manejo seguro mediante `ClientRequestException` en `ProductoViewModel.mensajeLegible()`.
- **Sin conexión (Offline):** Captura de `IOException` / fallo de red manteniendo la UI estable en estado `Error` con opción "Reintentar" sin caídas de la app.
- **Timeout:** Configurado con `HttpTimeout` (15s request / 10s connect).
- **Campo desconocido en JSON:** Tolerancia a cambios en el backend configurada con `ignoreUnknownKeys = true` y `isLenient = true`.