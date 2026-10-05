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

# Conectividad REST y CRUD Completo (Ktor Client + Spring Boot)

## Rama de trabajo
`feature/crud-productos-chambilla`

## Levantamiento de PharmaSoft (Backend Local)
1. Navegar al directorio del microservicio:
   ```bash
   cd "c:\Users\USER\Documents\ciclo 6\moviles\Pharmasoft\pharmaSoft"
   ```
2. Ejecutar Spring Boot:
   ```bash
   .\mvnw spring-boot:run
   ```
3. Verificar salud de la API:
   - Healthcheck: `http://localhost:8080/api/health`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`

## Configuración de Red por Plataforma
- **Android (Emulador AVD):** `http://10.0.2.2:8080/` (apunta al localhost del host de desarrollo).
- **iOS (Simulador Darwin) / Desktop:** `http://localhost:8080/`

## Catálogo de Endpoints CRUD Implementados
| Operación | Método | Ruta del Endpoint | Request Body | Respuesta Exitosa | Errores Manejados |
|---|:---:|---|---|:---:|---|
| **Listar** | `GET` | `/api/v1/productos` | Ninguno (Query params: `pagina`, `tamanio`) | `200 OK` (`PaginaResponseDto`) | `500` |
| **Obtener** | `GET` | `/api/v1/productos/{id}` | Ninguno | `200 OK` (`ProductoResponseDto`) | `400`, `404`, `500` |
| **Crear** | `POST` | `/api/v1/productos` | `ProductoRequestDto` | `201 Created` (`ProductoResponseDto`) | `400` (@Valid), `409` (Conflicto), `500` |
| **Actualizar** | `PUT` | `/api/v1/productos/{id}` | `ProductoRequestDto` | `200 OK` (`ProductoResponseDto`) | `400`, `404`, `409`, `500` |
| **Eliminar** | `DELETE` | `/api/v1/productos/{id}` | Ninguno | `204 No Content` (sin body) | `400`, `404`, `409`, `500` |

## Arquitectura de Capas
```text
UI (ProductoScreen) 
   ↓ (Observa ProductoUiState: Fase y Operación)
ViewModel (ProductoViewModel) [viewModelScope / StateFlow]
   ↓ (Invoca Casos de Uso del CRUD)
Casos de Uso (Listar, Obtener, Registrar, Actualizar, Eliminar)
   ↓ (Invoca ProductoRepository)
Repositorio (ProductoRepositorioRest)
   ↓ (ejecutarLlamada: traduce Ktor exceptions a ErrorApi sin atrapar CancellationException)
Servicio Remoto (ProductoApi)
   ↓ (Ktor HttpClient: GET, POST, PUT, DELETE tipados)
Microservicio REST Spring Boot (PharmaBackend: /api/v1/productos)
```

## DTOs (Data Transfer Objects)
- `ProductoRequestDto`: Datos para POST y PUT (`nombre`, `precio`, `stock`, `estado`, `categoriaId`).
- `ProductoResponseDto`: Datos devueltos por el servidor (`id`, `nombre`, `precio`, `stock`, `estado`, `categoriaId`, `categoriaNombre`, etc.).
- `PaginaResponseDto<T>`: Envoltorio genérico de paginación (`contenido`, `pagina`, `tamanio`, `totalElementos`, `totalPaginas`, `ultima`).
- `ErrorResponseDto`: Esquema de error del backend con `validationErrors` mapeados a los campos del formulario.

## Manejo de errores
Las capas de presentación y dominio están 100% aisladas de Ktor mediante la jerarquía sellada `ErrorApi`:
- `ErrorApi.Validacion`: Errores 400 por campo (`nombreError`, `precioError`, `stockError` visibles bajo los inputs).
- `ErrorApi.NoEncontrado`: HTTP 404.
- `ErrorApi.Conflicto`: HTTP 409 (regla de negocio / duplicados).
- `ErrorApi.Servidor`: HTTP 500.
- `ErrorApi.SinConexion`: Fallo de socket / offline.
- `ErrorApi.TiempoAgotado`: `HttpRequestTimeoutException`.

## Estados de UI (`ProductoUiState`)
- **Fase de Pantalla:** `Cargando`, `SinProductos`, `ConProductos(productos)`, `Error(mensaje)`.
- **Operación Concurrente:** `Inactiva`, `EnCurso(Tipo: Crear, Actualizar, Eliminar)`, `Fallida(mensaje)`.
- Al crear, actualizar o eliminar, la lista de productos **permanece visible** sin destruirse ni volver a pantalla completa de carga, y se recarga automáticamente tras la mutación.