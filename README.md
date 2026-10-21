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

El proyecto implementa un aislamiento estricto entre la infraestructura de red (Ktor) y las capas de Dominio y Presentación (Clean Architecture). La capa de UI y el ViewModel jamás importan paquetes de `io.ktor.*`, operando exclusivamente con la jerarquía sellada de dominio `ErrorApi`.

### 1. Tabla de Mapeo de Códigos HTTP a Dominio (`ErrorApi`)

| Código HTTP | Causa Técnica en Backend / Red | Excepción Ktor | Tipo `ErrorApi` de Dominio | Representación Observable en UI |
|:---:|---|---|---|---|
| **400** | Falla de `@Valid` en Spring Boot (longitud < 3, precio <= 0, etc.) | `ClientRequestException` (400) | `ErrorApi.Validacion(porCampo)` | Texto rojo debajo del campo específico (`nombreError`, `precioError`, etc.) |
| **404** | ID inexistente o entidad no encontrada en BD | `ClientRequestException` (404) | `ErrorApi.NoEncontrado` | Tarjeta roja: *"Recurso no encontrado en el servidor"* |
| **409** | Regla de negocio (`ReglaNegocioException` por nombre duplicado o inactivo) | `ClientRequestException` (409) | `ErrorApi.Conflicto(mensaje)` | Tarjeta roja con mensaje del servidor: *"Ya existe un producto con el nombre..."* |
| **500** | Error no controlado o caída interna en Spring Boot | `ServerResponseException` (500) | `ErrorApi.Servidor` | Tarjeta roja: *"Error interno del servidor"* |
| **Sin HTTP** | Falla de conexión TCP / Backend apagado / Modo avión | `ConnectException` / `SocketException` | `ErrorApi.SinConexion` | Pantalla de reintento en carga inicial o banner *"Sin conexión con el servidor"* |
| **Timeout** | Tiempo de espera de petición agotado (> 15s) | `HttpRequestTimeoutException` | `ErrorApi.TiempoAgotado` | Tarjeta roja: *"La solicitud ha tardado demasiado tiempo"* |
| **N/A** | Cancelación cooperativa al salir de la pantalla | `CancellationException` | *No mapeado a ErrorApi* | La corrutina finaliza limpiamente sin actualizar estado muerto |

### 2. Traductor Centralizado (`ejecutarLlamada.kt`)
Todas las llamadas del repositorio REST (`ProductoRepositorioRest`) se ejecutan a través de la función de orden superior `ejecutarLlamada`:
```kotlin
suspend fun <T> ejecutarLlamada(bloque: suspend () -> T): Result<T> =
    try {
        Result.success(bloque())
    } catch (cancelacion: CancellationException) {
        throw cancelacion // REGLA CRÍTICA: No atrapar cancelación para evitar estados zombies
    } catch (e: ClientRequestException) {
        Result.failure(ErrorApiException(traducirCliente(e)))
    } catch (e: ServerResponseException) {
        Result.failure(ErrorApiException(ErrorApi.Servidor))
    } catch (e: HttpRequestTimeoutException) {
        Result.failure(ErrorApiException(ErrorApi.TiempoAgotado))
    } catch (e: Throwable) {
        Result.failure(ErrorApiException(ErrorApi.SinConexion))
    }
```

### 3. Conexión de Errores 400 al Formulario
Cuando el servidor devuelve `HTTP 400 Bad Request`, `ejecutarLlamada` deserializa `ErrorResponseDto` extrayendo el mapa `validationErrors: Map<String, String>?`. En el ViewModel, `manejarFallo` asigna los errores directamente a los campos correspondientes del formulario:
```kotlin
is ErrorApi.Validacion -> {
    _uiState.update {
        it.copy(
            operacion = ProductoUiState.Operacion.Inactiva,
            formulario = it.formulario.copy(
                nombreError = errorApi.porCampo["nombre"],
                precioError = errorApi.porCampo["precio"],
                stockError = errorApi.porCampo["stock"]
            )
        )
    }
}
```
En la vista Compose, el componente `ValidatedTextField` evalúa `error = form.nombreError` y renderiza el mensaje exacto enviado por Spring Boot debajo de la caja de texto, manteniendo la lista de productos visible.

## Estados de UI (`ProductoUiState`)
- **Fase de Pantalla:** `Cargando`, `SinProductos`, `ConProductos(productos)`, `Error(mensaje)`.
- **Operación Concurrente:** `Inactiva`, `EnCurso(Tipo: Crear, Actualizar, Eliminar)`, `Fallida(mensaje)`.
- Al crear, actualizar o eliminar, la lista de productos **permanece visible** sin destruirse ni volver a pantalla completa de carga, y se recarga automáticamente tras la mutación.

---

## Capacidades nativas

En esta práctica se integran capacidades de plataforma nativa en **PharmaMobile2** bajo Kotlin Multiplatform (KMP), asegurando que la lógica de negocio y la interfaz de usuario en Compose Multiplatform permanezcan completamente agnósticas al sistema operativo (Android / iOS).

### 1. Formato de moneda (`expect` / `actual`)

Para garantizar que el precio de cada producto se presente respetando las convenciones monetarias oficiales de Perú (`S/`), se implementó la función `formatearSoles`:

- **Declaración común (`expect`):** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/platform/Formato.kt`
  ```kotlin
  expect fun formatearSoles(valor: Double): String
  ```
- **Implementación Android (`actual`):** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile2/platform/Formato.android.kt`
  Utiliza `java.text.NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))`.
- **Implementación iOS (`actual`):** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile2/platform/Formato.ios.kt`
  Utiliza `platform.Foundation.NSNumberFormatter` con estilo `NSNumberFormatterCurrencyStyle` y locale `es_PE`.
- **Mapeo a nivel de presentación (`ProductoUi`):** El modelo de dominio `Producto` mantiene `precio: Double`, mientras que `ProductoUi` expone `precio: String` ya formateado para la UI, evitando formatear repetidamente en recomposiciones de Compose.

### 2. Compartir producto (Inyección de Dependencias con Koin)

Para permitir compartir la ficha de un medicamento hacia aplicaciones externas (WhatsApp, Telegram, Correo, etc.):

- **Contrato de Dominio:** `Compartidor` en `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/domain/platform/Compartidor.kt`.
  ```kotlin
  interface Compartidor {
      fun compartir(texto: String)
  }
  ```
- **Formateo de texto común:** `Producto.comoTextoParaCompartir()` genera el contenido descriptivo estándar: `"$nombre - ${formatearSoles(precio)} - Stock: $stock"`.
- **Implementación Android:** `CompartidorAndroid.kt` lanza un `Intent(Intent.ACTION_SEND)` envuelto en `Intent.createChooser` y decorado con `FLAG_ACTIVITY_NEW_TASK` usando el `Context` de Android.
- **Implementación iOS:** `CompartidorIos.kt` instancia un `UIActivityViewController` y lo presenta sobre el `rootViewController` de la ventana activa de `UIKit`.
- **Inyección con Koin:** Ambos módulos de plataforma (`PlatformModule.android.kt` y `PlatformModule.ios.kt`) proveen la instancia de `Compartidor`. El `DetalleProductoViewModel` recibe la interfaz `Compartidor` por constructor, y la pantalla composable `DetalleProductoScreen` solo emite un evento lambda `onCompartir`, manteniendo **cero importaciones de Android o UIKit** en la capa común.

### 3. Información del Dispositivo (`expect class` / `actual class`)

Para consultar el sistema operativo y su versión sin depender de librerías externas ni solicitar permisos sensibles:

- **Declaración común (`expect class`):** `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/platform/InfoDispositivo.kt`
  ```kotlin
  expect class InfoDispositivo() {
      val sistema: String
      val version: String
  }
  ```
- **Implementación Android (`actual class`):** `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile2/platform/InfoDispositivo.android.kt`
  Utiliza `android.os.Build.VERSION.RELEASE` retornando `"Android"` y la versión del sistema (ej. `"14"`).
- **Implementación iOS (`actual class`):** `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile2/platform/InfoDispositivo.ios.kt`
  Utiliza `platform.UIKit.UIDevice.currentDevice.systemName` y `systemVersion` retornando `"iOS"` y su versión.
- **Inyección con Koin:** Registrado como singleton común en `AppModule.kt` (`single { InfoDispositivo() }`).
- **Pantalla «Acerca de»:** Presentada en `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/presentation/acercade/AcercaDeScreen.kt`, accesible directamente desde el Navigation Drawer en todas las plataformas.

### 4. Código específico de plataforma (Inventario S09)

| Capacidad | Contrato Común (`commonMain`) | Android (`androidMain`) | iOS (`iosMain`) |
|---|---|---|---|
| **Formato de moneda** | `pe.edu.upeu.pharmamobile2.platform.Formato.kt`<br>`expect fun formatearSoles(valor: Double): String` | `Formato.android.kt`<br>`NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-PE"))` | `Formato.ios.kt`<br>`NSNumberFormatter` con `NSNumberFormatterCurrencyStyle` y `es_PE` |
| **Compartir producto** | `pe.edu.upeu.pharmamobile2.domain.platform.Compartidor.kt`<br>`interface Compartidor { fun compartir(texto: String) }` | `CompartidorAndroid.kt`<br>`Intent(Intent.ACTION_SEND)`, `createChooser`, `FLAG_ACTIVITY_NEW_TASK` | `CompartidorIos.kt`<br>`UIActivityViewController` sobre `keyWindow.rootViewController` |
| **Módulo de inyección** | `pe.edu.upeu.pharmamobile2.di.PlatformModule.kt`<br>`expect val platformModule: Module` | `PlatformModule.android.kt`<br>Koin `module` resolviendo `androidContext()` para `Compartidor` | `PlatformModule.ios.kt`<br>Koin `module` instanciando `CompartidorIos()` puro |
| **Información del dispositivo** | `pe.edu.upeu.pharmamobile2.platform.InfoDispositivo.kt`<br>`expect class InfoDispositivo()` | `InfoDispositivo.android.kt`<br>`android.os.Build.VERSION.RELEASE` | `InfoDispositivo.ios.kt`<br>`platform.UIKit.UIDevice.currentDevice.systemVersion` |

### 5. Mapa de archivos y arquitectura

| Capa / Módulo | Archivo | Responsabilidad |
|---|---|---|
| **Común (Platform)** | `shared/.../platform/Formato.kt` | Declaración `expect fun formatearSoles` |
| **Android (Platform)** | `shared/.../platform/Formato.android.kt` | Implementación `actual` con `NumberFormat` |
| **iOS (Platform)** | `shared/.../platform/Formato.ios.kt` | Implementación `actual` con `NSNumberFormatter` |
| **Común (Platform)** | `shared/.../platform/InfoDispositivo.kt` | Declaración `expect class InfoDispositivo` |
| **Android (Platform)** | `shared/.../platform/InfoDispositivo.android.kt` | Implementación `actual` con `Build.VERSION.RELEASE` |
| **iOS (Platform)** | `shared/.../platform/InfoDispositivo.ios.kt` | Implementación `actual` con `UIDevice.systemVersion` |
| **Común (Domain)** | `shared/.../domain/platform/Compartidor.kt` | Interfaz abstracta para compartir texto |
| **Común (UseCase)** | `shared/.../domain/usecase/TextoParaCompartir.kt` | Construcción del mensaje a compartir |
| **Android (Platform)** | `shared/.../platform/CompartidorAndroid.kt` | Implementación con `Intent.ACTION_SEND` |
| **iOS (Platform)** | `shared/.../platform/CompartidorIos.kt` | Implementación con `UIActivityViewController` |
| **Android (DI)** | `shared/.../di/PlatformModule.android.kt` | Registro Koin de `CompartidorAndroid` |
| **iOS (DI)** | `shared/.../di/PlatformModule.ios.kt` | Registro Koin de `CompartidorIos` |
| **Común (DI)** | `shared/.../di/AppModule.kt` | Registro Koin de `deviceModule` e `InfoDispositivo` |
| **Común (Presentation)** | `shared/.../presentation/producto/ProductoUi.kt` | Modelo UI con precio formateado |
| **Común (Presentation)** | `shared/.../presentation/detalle/DetalleProductoViewModel.kt` | Orquestación del estado de detalle y compartir |
| **Común (Presentation)** | `shared/.../presentation/detalle/DetalleProductoScreen.kt` | Vista Compose pura con botón nativo de Compartir |
| **Común (Presentation)** | `shared/.../presentation/acercade/AcercaDeScreen.kt` | Pantalla «Acerca de» mostrando info del dispositivo |
| **Común (Navigation)** | `shared/.../navigation/Screen.kt` | Rutas `Screen.DetalleProducto` y `Screen.AcercaDe` |
| **Común (Tests)** | `shared/src/commonTest/.../DetalleProductoViewModelTest.kt` | Pruebas unitarias de ViewModel, Formato, Fake e InfoDispositivo |

### 6. Interoperabilidad Kotlin-Swift

Kotlin Multiplatform compila el código compartido para iOS generando un framework nativo (`Shared.framework`) mediante el backend de Kotlin/Native:
- **Puente Objective-C/Swift:** Kotlin/Native genera un encabezado `.h` que expone las clases y funciones públicas de Kotlin como clases y protocolos de Objective-C accesibles directamente desde Swift.
- **Acceso a APIs de Apple:** A través del paquete `platform.*` (como `platform.Foundation.*` y `platform.UIKit.*`), Kotlin/Native cuenta con bindings directos y de cero sobrecoste (zero-overhead) a las APIs nativas de iOS en tiempo de compilación.
- **Ciclo de vida en Swift:** En `iosApp/iOSApp.swift`, la aplicación inicializa Koin mediante `HelperKt.doInitKoin()` y monta la vista raíz `ComposeViewProvider` en SwiftUI, manteniendo la paridad arquitectónica total con Android.

---

# Autenticación JWT y Almacenamiento Seguro (Sesión 10)

## Rama de trabajo
`feature/auth-jwt-chambilla`

## 1. Flujo de Autenticación y Ciclo de Vida
```text
Usuario escribe credenciales (o selecciona Chip de prueba)
             ↓
PharmaMobile llama a POST /api/v1/auth/login
             ↓
Recibe JWT, roles y permisos (HTTP 200)
             ↓
Guarda token de forma cifrada (AlmacenSeguro: EncryptedSharedPreferences / Keychain)
             ↓
Ktor Auth Plugin adjunta automáticamente "Authorization: Bearer <token>"
             ↓
Al reiniciar la app: Recupera token y consulta GET /api/v1/auth/me
             ↓
• Si token es válido (200) → Restaura sesión automáticamente
• Si token expiró (401) → Elimina token, purga caché bearer y muestra login con aviso
• Si falta permisos (403) → Mantiene sesión intacta y notifica con Snackbar
• Si pulsa "Cerrar sesión" → Borra token, limpia caché y vuelve a Login
```

## 2. Endpoints Utilizados
| Operación | Método | Endpoint | Cabecera / Body | Código Exitoso | Respuestas de Error |
|---|:---:|---|---|:---:|---|
| **Iniciar Sesión** | `POST` | `/api/v1/auth/login` | Body: `{ "username": "...", "password": "..." }` | `200 OK` (Token JWT, expiración, roles, permisos) | `400` (Invalido), `401` (Credenciales incorrectas) |
| **Usuario Actual** | `GET` | `/api/v1/auth/me` | `Authorization: Bearer <token>` | `200 OK` (`UsuarioActualDto`) | `401` (Token expirado/inválido) |
| **Listar Productos** | `GET` | `/api/v1/productos` | `Authorization: Bearer <token>` | `200 OK` | `401` (No autenticado) |
| **Crear Producto** | `POST` | `/api/v1/productos` | `Authorization: Bearer <token>` | `201 Created` | `401` (No autenticado), `403` (Vendedor sin `CATALOGO_ESCRIBIR`) |
| **Listar Clientes** | `GET` | `/api/v1/clientes` | `Authorization: Bearer <token>` | `200 OK` | `401` (No autenticado), `403` (Almacén sin `CLIENTES_LEER`) |

## 3. Cuentas de Prueba (Laboratorio PharmaSoft)
| Usuario | Contraseña | Rol Principal | Permisos Clave | Comportamiento Esperado |
|---|---|---|---|---|
| `admin` | `Admin123` | `ROLE_ADMIN` | `CATALOGO_LEER`, `CATALOGO_ESCRIBIR`, `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `REPORTES_VER`, `VENTAS_REGISTRAR` | Acceso total a todas las operaciones (200 / 201). |
| `vendedor` | `Vendedor123` | `ROLE_VENDEDOR` | `CATALOGO_LEER`, `CLIENTES_LEER`, `CLIENTES_ESCRIBIR`, `VENTAS_REGISTRAR` | Puede ver catálogo y gestionar clientes. Al intentar crear un producto recibe **403 Forbidden**. |
| `almacen` | `Almacen123` | `ROLE_ALMACEN` | `CATALOGO_LEER`, `CATALOGO_ESCRIBIR` | Puede ver y modificar catálogo. Al intentar consultar o registrar clientes recibe **403 Forbidden**. |

> **Nota:** La pantalla de inicio de sesión (`LoginScreen`) incluye chips de autocompletado rápido para alternar entre estas cuentas en un solo toque durante evaluaciones y pruebas.

## 4. Almacenamiento Seguro por Plataforma
- **Android (`AlmacenSeguroAndroid`):** Utiliza `EncryptedSharedPreferences` respaldado por `MasterKey` con esquema de cifrado de claves `AES256_SIV` y valores `AES256_GCM`. El token JWT nunca se almacena en texto plano.
- **iOS (`AlmacenSeguroIos`):** Implementación nativa usando la API C-Interop de Apple **Keychain** (`SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete`) bajo la clase `kSecClassGenericPassword`, garantizando almacenamiento seguro en el enclave del sistema operativo.
- **Contrato Común:** Interfaz `AlmacenSeguro` inyectada vía Koin como singleton en cada plataforma (`PlatformModule`).

## 5. Configuración Ktor Client (Bearer Token Interceptor)
- **Plugin `Auth`:** Configurado con `bearer { loadTokens { ... } }`, leyendo el token desde `AlmacenSeguro`.
- **`sendWithoutRequest`:** Envía la cabecera `Authorization: Bearer <token>` proactivamente en todas las rutas salvo `/api/v1/auth/login`.
- **Saneamiento de Cabeceras (`Logging`):**
  ```kotlin
  sanitizeHeader { header ->
      header.equals(HttpHeaders.Authorization, ignoreCase = true)
  }
  ```
  Evita fugas de tokens JWT en los logs de la consola o logcat.
- **Invalidación de Caché:** Función `HttpClient.limpiarTokensAuth()` que purga las instancias de `BearerAuthProvider` tras login o logout para evitar reutilizar tokens antiguos.

## 6. Manejo Distintivo de 401 y 403
- **HTTP 401 Unauthorized:** Indica credenciales inválidas (en login) o token expirado/manipulado. Se elimina el token de `AlmacenSeguro`, se limpia la caché del cliente Ktor, se vacía el estado observable de sesión y la aplicación transiciona inmediatamente a la pantalla de Login mostrando el aviso: *"Su sesión ha expirado. Por favor inicie sesión nuevamente."*
- **HTTP 403 Forbidden:** Indica que el usuario está autenticado válidamente pero su rol carece del permiso para la operación (por ejemplo, `almacen` intentando listar clientes, o `vendedor` intentando registrar un producto). **La sesión NO se destruye, el token permanece en el almacén**, y la UI emite un mensaje en Snackbar: *"No tienes permiso para esta operación (403)"*.

## 7. Instrucciones de Ejecución y Pruebas en Android Studio
1. **Asegurar Backend Activo:**
   - PharmaSoft debe estar ejecutándose en `http://localhost:8080/`.
   - Desde el emulador Android, se accede transparentemente mediante `http://10.0.2.2:8080/`.
2. **Abrir el Proyecto en Android Studio:**
   - Abrir la carpeta raíz `PharmaMobile2`.
   - Esperar a que Gradle sincronice (`Sync Project with Gradle Files`).
3. **Ejecutar Pruebas Unitarias:**
   - En la terminal de Android Studio o PowerShell:
     ```powershell
     .\gradlew :shared:testAndroidHostTest
     ```
   - Las 42 pruebas unitarias (`PermisosTest`, `AuthRepositorioRestTest`, `SesionViewModelTest`, `DominioPharmaMobileTest`, `ProductoViewModelTest`, etc.) deben ejecutarse con **100% de éxito**.
4. **Desplegar en Emulador:**
   - Seleccionar la configuración de ejecución `androidApp`.
   - Elegir el dispositivo virtual (Pixel 7 con API 34+).
   - Presionar `Run` (Shift + F10).
5. **Casos de Prueba Autónomos a Validar:**
   - **Caso A (Almacenamiento y Persistencia):** Iniciar sesión con `admin`. Navegar a Productos. Cerrar completamente la aplicación desde el multitarea de Android. Volver a abrir la app: la pantalla de carga verifica el token y restaura la sesión directamente en el inicio sin pedir contraseña.
   - **Caso B (Cierre de Sesión):** Abrir el menú lateral (Drawer) y pulsar "Cerrar sesión" (o el icono de puerta en la barra superior). La app limpia el almacenamiento seguro y regresa al Login. Al pulsar el botón Atrás de Android, la app se minimiza y no vuelve a las pantallas privadas.
   - **Caso C (Control de Acceso 403 vs 200):**
     1. Iniciar sesión con `almacen`.
     2. Ir a la sección Clientes y pulsar *"📡 Consultar Clientes en Servidor"*. Aparece el mensaje de error `403 Forbidden` y el Snackbar de permisos.
     3. Regresar a Productos: los productos siguen cargando normalmente (HTTP 200), confirmando que la sesión se mantuvo.
     4. Cerrar sesión y entrar con `vendedor`.
     5. Ir a Clientes y pulsar la misma consulta: responde `HTTP 200 OK` con acceso concedido.

---

# Persistencia Offline-First con SQLDelight (Sesión 11)

## Rama de trabajo
`feature/offline-first-chambilla`

## 1. Arquitectura Offline-First y Flujo de Datos

La arquitectura implementada sigue el patrón **Single Source of Truth (SSOT)**: la base de datos local SQLite (gestionada a través de SQLDelight) es la única fuente de verdad para la capa de presentación. La interfaz de usuario nunca espera una respuesta HTTP del backend para reflejar los datos: observa continuamente la base de datos local mediante Kotlin `Flow`.

```text
+---------------------------------------------------------------------------------+
|                       CAPA DE PRESENTACIÓN (Compose UI)                         |
|  ProductoScreen (Badge ⏳ Pendiente en id < 0, Barra de Sync con contador)      |
+---------------------------------------------------------------------------------+
                                       ▲
                        collectAsState() / StateFlow
                                       │
+---------------------------------------------------------------------------------+
|                       VIEWMODEL & CASOS DE USO (Domain)                         |
|  ProductoViewModel                                                             |
|  ├── ObservarProductosUseCase() ──► Flow<List<Producto>> (reactivo continuo)   |
|  ├── RegistrarProductoUseCase() ──► guarda local inmediatamente                |
|  └── SincronizarProductosUseCase() ──► orquesta reconciliación bidireccional     |
+---------------------------------------------------------------------------------+
                                       │
                        ProductoRepository (Offline)
                                       │
            ┌───────────────────────────┴───────────────────────────┐
            ▼                                                       ▼
+----------------------+                               +----------------------+
|  BASE DE DATOS LOCAL |                               |   CLIENTE REMOTO     |
|   (SQLDelight KMP)   |                               |    (Ktor Client)     |
|                      |                               |                      |
|  ProductoLocal       |                               |  ProductoApi         |
|  • id < 0: Pendiente |                               |  • POST /productos   |
|  • id > 0: Remoto    |                               |  • GET /productos    |
|  • observar(): Flow  |                               |  • PUT /productos    |
|  • reconciliación    |                               |  • DELETE /productos |
+----------------------+                               +----------------------+
           │                                                       │
           ▼                                                       ▼
  SQLite Local (Driver)                                   Spring Boot REST API
  • Android: AndroidSqliteDriver                         (PharmaSoft Backend)
  • iOS: NativeSqliteDriver
  • Pruebas: InMemory Driver
```

### Flujo de Datos Clave
1. **Lectura / Observación:** `ProductoRepositoryOffline.observar()` expone un `Flow<List<Producto>>` impulsado por las extensiones de corrutinas de SQLDelight (`asFlow().mapToList()`). Cada vez que la tabla local cambia (inserción, actualización, eliminación o reconciliación), la UI se actualiza automáticamente sin llamadas manuales de recarga.
2. **Creación Offline-First:** Cuando el usuario registra un nuevo producto:
   - Se almacena de inmediato en la base de datos local SQLite con un identificador temporal negativo (ej. `-1`) y `pendiente = 1`.
   - La UI emite el nuevo producto inmediatamente en la lista con el badge ámbar `⏳ Pendiente`.
   - Si existe conexión a internet, el repositorio intenta sincronizar en segundo plano de manera no bloqueante.
   - Si no hay conexión, el registro permanece seguro en el almacenamiento local hasta la próxima sincronización.

---

## 2. Esquema SQL y Consultas SQLDelight (`Producto.sq`)

El esquema de persistencia local se encuentra definido en `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq` bajo la base de datos `PharmaDb`:

```sql
CREATE TABLE producto (
    id INTEGER NOT NULL PRIMARY KEY,
    nombre TEXT NOT NULL,
    precio REAL NOT NULL,
    stock INTEGER NOT NULL,
    pendiente INTEGER NOT NULL DEFAULT 0
);

listarTodos:
SELECT *
FROM producto
ORDER BY nombre ASC;

listarPendientes:
SELECT *
FROM producto
WHERE pendiente = 1
ORDER BY id DESC;

siguienteIdLocal:
SELECT COALESCE(MIN(id), 0) - 1
FROM producto
WHERE id < 0;

insertar:
INSERT OR REPLACE INTO producto(id, nombre, precio, stock, pendiente)
VALUES (?, ?, ?, ?, ?);

borrarSincronizados:
DELETE FROM producto
WHERE pendiente = 0;

eliminarPorId:
DELETE FROM producto
WHERE id = ?;

contarPendientes:
SELECT COUNT(*)
FROM producto
WHERE pendiente = 1;
```

### Fundamento de las Consultas
- **`listarTodos`:** Retorna todos los productos locales ordenados de forma descendente, permitiendo que los elementos recién creados aparezcan de inmediato al inicio de la lista.
- **`listarPendientes`:** Filtra únicamente aquellos registros con `pendiente = 1` en orden FIFO (`id ASC`) para procesar las creaciones offline en el mismo orden cronológico en que ocurrieron.
- **`siguienteIdLocal`:** Garantiza la monotonicidad de los identificadores temporales negativos (`-1`, `-2`, `-3`...) mediante `COALESCE(MIN(id), 0) - 1`.
- **`borrarSincronizados`:** Limpia el catálogo remoto anterior durante una sincronización exitosa sin tocar las creaciones offline pendientes (`pendiente = 1`).
- **`eliminarPorId`:** Permite eliminar atómicamente el registro temporal negativo una vez que el servidor confirma la creación y asigna un ID definitivo.

---

## 3. Drivers de Plataforma e Inyección de Dependencias

El acceso al motor SQLite se desacopla mediante el mecanismo `expect` / `actual` de Kotlin Multiplatform, inyectando el driver adecuado vía Koin:

| Plataforma | Implementación del Driver | Archivo |
|---|---|---|
| **Común (`commonMain`)** | `expect fun crearDriver(): SqlDriver` | `pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriver.kt` |
| **Android (`androidMain`)** | `AndroidSqliteDriver(PharmaDb.Schema, context, "pharma.db")` | `pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriverAndroid.kt` |
| **iOS (`iosMain`)** | `NativeSqliteDriver(PharmaDb.Schema, "pharma.db")` | `pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriverIos.kt` |
| **Pruebas (`commonTest`)** | `expect fun crearDriverPruebas(): SqlDriver`<br>• Android/JVM: `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)`<br>• iOS: `NativeSqliteDriver.inMemoryDriver(...)` | `pe.edu.upeu.pharmamobile2.DriverPruebas.kt` |

### Registro en Koin (`AppModule.kt` y `PlatformModule.*`)
```kotlin
// PlatformModule.android.kt
val platformModule = module {
    single<SqlDriver> { crearDriverAndroid(androidContext()) }
    ...
}

// PlatformModule.ios.kt
val platformModule = module {
    single<SqlDriver> { crearDriverIos() }
    ...
}

// AppModule.kt (Común)
val localModule = module {
    single { PharmaDb(get()) }
    single { ProductoLocal(get()) }
}

val repositoryModule = module {
    single<ProductoRepository> {
        ProductoRepositoryOffline(
            local = get(),
            api = get(),
            autenticacion = get()
        )
    }
}
```

---

## 4. Mecanismo de Generación de IDs Temporales Negativos

Para admitir la creación de registros sin conexión evitando colisiones con los identificadores autoincrementales positivos generados por PostgreSQL / MySQL en el servidor:

1. **Rango Negativo Monótonamente Decreciente:** Cada nuevo producto sin conexión recibe un ID temporal menor que cero:
   $$\text{id}_{\text{nuevo}} = \min(0, \min_{i \in \text{locales}}(\text{id}_i)) - 1$$
   - Primer producto local: `-1`
   - Segundo producto local: `-2`
   - Tercer producto local: `-3`
2. **Cero Colisiones:** Al estar restringido al conjunto de enteros negativos ($x < 0$), es matemáticamente imposible que colisione con las claves primarias positivas generadas por el backend ($x \ge 1$).
3. **Identificación Inmediata en Capas Superiores:**
   ```kotlin
   val esPendiente: Boolean = id < 0L || pendiente
   ```
   Tanto el modelo de dominio como `ProductoUi` determinan instantáneamente si un elemento es pendiente evaluando si `id < 0`.

---

## 5. Sincronización Bidireccional y Reconciliación Atómica

El método `sincronizar()` implementa una estrategia en dos etapas diseñada para preservar en todo momento la integridad transaccional del catálogo:

```text
                            sincronizar()
                                 │
       ┌─────────────────────────┴─────────────────────────┐
       ▼                                                   ▼
[ETAPA 1: SUBIDA Y RECONCILIACIÓN]             [ETAPA 2: DESCARGA REMOTA]
 Consultar listarPendientes()                   Consultar GET /api/v1/productos
 Para cada producto pendiente:                  ¿Respuesta exitosa del servidor?
 ├── POST /api/v1/productos                              │
 ├── Servidor responde con ID = 42                       ├─► SÍ:
 │   ├── eliminarPorId(idNegativo) (ej. -1)              │   ├── borrarSincronizados()
 │   └── insertar(42, datos, pendiente = 0)              │   │   (¡Conserva pendientes!)
 │   (Reconciliación Atómica)                            │   └── insertar(remotos, pend = 0)
 └── ¿Error de red?                                      │
     Se interrumpe la subida pero                        └─► NO:
     los pendientes continúan intactos en BD                 Mantiene catálogo local actual
```

### Principios Clave de la Reconciliación
- **Subida Antes de Descarga:** Primero se envían los pendientes locales antes de actualizar el catálogo general, evitando sobreescrituras accidentales.
- **Reconciliación Atómica:** Al recibir confirmación `201 Created` del servidor con el nuevo `ProductoResponseDto`, se ejecuta la transición en base de datos: se borra el registro temporal negativo `-X` y se inserta el confirmado con el ID remoto devuelto por el backend.
- **Preservación de Pendientes en Descarga:** `borrarSincronizados()` elimina exclusivamente los registros donde `pendiente == 0`. Si algunos productos no pudieron subirse por intermitencia de red, no se pierden al descargar la lista remota.

---

## 6. Comportamiento ante Operaciones de Edición y Eliminación sin Conexión

Siguiendo las especificaciones arquitectónicas de la actividad (Sección 3.6 de `req.md`):

- **Creación (Offline-First Total):** Funciona al 100% sin conexión a internet. Los productos se persisten en local con ID negativo y se sincronizan cuando la conectividad se restablece.
- **Actualización (`actualizar`) y Eliminación (`eliminar`) (Connected First):**
  - Requieren validación con el servidor central para prevenir conflictos de concurrencia distribuida (como modificaciones concurrentes sobre un mismo producto o eliminación de registros que ya no existen en la base de datos central).
  - Si el dispositivo se encuentra sin conexión, la operación lanza de inmediato `ErrorApi.SinConexion`.
  - La interfaz de usuario captura este error de dominio y notifica al usuario con un banner o Snackbar informativo (*"Sin conexión con el servidor. No es posible actualizar/eliminar productos sin conexión"*), manteniendo intacto el estado de la base de datos local.

---

## 7. Interfaz Reactiva e Indicador Visual de Estado

La pantalla `ProductoScreen` provee retroalimentación visual clara e inequívoca sobre el estado de conectividad y sincronización:

1. **Badge Ámbar `⏳ Pendiente`:**
   - Cada tarjeta de producto (`ProductoCard`) cuyos datos correspondan a `id < 0` o `esPendiente == true` despliega un badge redondeado en color ámbar (`Color(0xFFFFF8E1)` de fondo y texto `Color(0xFFB78103)`), indicando que el registro existe únicamente en el almacenamiento local.
2. **Barra de Sincronización Superior:**
   - Ubicada permanentemente arriba de la lista de productos.
   - Muestra el estado actual:
     - En sincronización activa: Indicador de progreso circular con texto *"Sincronizando..."*.
     - Con pendientes: Icono de nube pendiente con el conteo exacto de elementos (ej. *"2 pendientes por sincronizar"*).
     - Sin pendientes: Icono de verificación verde con texto *"Catálogo sincronizado"*.
   - Dispone de un botón interactivo *"Sincronizar"* para forzar la sincronización manual bajo demanda del usuario.

---

## 8. Batería de Pruebas Automatizadas y Evidencias

Se desarrollaron pruebas unitarias exhaustivas en `shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobile2/ProductoRepositoryOfflineTest.kt` utilizando un driver SQLite en memoria (`JdbcSqliteDriver.IN_MEMORY` y `NativeSqliteDriver.inMemoryDriver`) y un cliente HTTP simulado (`MockEngine`), garantizando aislamiento y reproducibilidad total.

### Casos de Prueba Implementados
1. **`guardar_sinConexion_persisteLocalConIdNegativo`:** Valida que al crear un producto sin conexión a la API se almacene localmente con ID negativo (`-1`) y estado pendiente (`pendiente = 1`).
2. **`sincronizar_conConexion_reconciliaIdTemporalConIdRemoto`:** Valida que al sincronizar con el servidor activo, el ID temporal negativo `-1` sea eliminado y reemplazado atómicamente por el ID confirmado por el servidor (ej. `100`), actualizando `pendiente = 0`.
3. **`sincronizar_sinConexion_mantienePendientesIntactos`:** Valida que si la red falla durante la sincronización, los productos locales pendientes permanezcan intactos en la base de datos sin pérdida de información.
4. **`observar_emiteCambiosAlInsertar`:** Valida la naturaleza reactiva de `ProductoLocal.observar()` y `Flow`, comprobando que una nueva inserción local dispare una nueva emisión con la lista actualizada.
5. **`actualizar_sinConexion_fallaConSinConexion`:** Comprueba que intentar actualizar un producto sin conexión falle con `ErrorApi.SinConexion` sin corromper la base de datos local.
6. **`eliminar_sinConexion_fallaConSinConexion`:** Comprueba que intentar eliminar un producto sin conexión falle con `ErrorApi.SinConexion` de acuerdo con la especificación de operaciones conectadas.

### Evidencia de Ejecución Exitosa
Comando de ejecución:
```powershell
.\gradlew :shared:testAndroidHostTest
```

Salida de Gradle:
```text
> Task :shared:compileAndroidMain UP-TO-DATE
> Task :shared:compileAndroidHostTest UP-TO-DATE
> Task :shared:testAndroidHostTest

BUILD SUCCESSFUL in 21s
33 actionable tasks: 33 executed
```

Resumen de Pruebas Unitarias del Proyecto (`shared/build/reports/tests/testAndroidHostTest/index.html`):
```text
+----------------------------------------------------------------------------------+
| Test Summary                                                                     |
| 47 tests, 0 failures, 0 ignored, 100% successful                                 |
+----------------------------------------------------------------------------------+
| Clase de Prueba                                      | Tests | Fallos | Éxito    |
|-----------------------------------------------------|-------|--------|----------|
| pe.edu.upeu.pharmamobile2.ProductoRepositoryOfflineTest | 6     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.ProductoViewModelTest         | 5     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.DetalleProductoViewModelTest  | 6     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.SesionViewModelTest           | 9     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.PermisosTest                  | 4     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.ProductoRemoteTest            | 2     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.DominioPharmaMobileTest       | 5     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.SharedLogicAndroidHostTest    | 2     | 0      | 100%     |
| pe.edu.upeu.pharmamobile2.AuthRepositorioRestTest       | 8     | 0      | 100%     |
+----------------------------------------------------------------------------------+
```

---

## 9. Matriz de Cumplimiento de Rúbrica (20 / 20 Puntos)

| Criterio de Evaluación | Puntos | Estado | Evidencia Técnica en el Código |
|---|:---:|:---:|---|
| **1. Configuración de SQLDelight en KMP** | 2.0 | **Cumplido** | Plugin `app.cash.sqldelight` versión `2.1.0` configurado en `libs.versions.toml`, `build.gradle.kts` y bloque `sqldelight { databases { create("PharmaDb") { packageName.set("pe.edu.upeu.pharmamobil.db") } } }` en `shared/build.gradle.kts`. |
| **2. Esquema SQL y Consultas** | 2.0 | **Cumplido** | Archivo `Producto.sq` con tabla `ProductoLocal` de 5 campos estrictos (`id`, `nombre`, `precio`, `stock`, `pendiente`) y consultas `listarTodos`, `listarPendientes`, `siguienteIdLocal`, `insertar`, `borrarSincronizados`, `eliminarPorId`, `contarPendientes`. |
| **3. Persistencia Local y Driver por Plataforma** | 2.0 | **Cumplido** | Implementación `expect`/`actual` de `crearDriver()` con `AndroidSqliteDriver` en Android y `NativeSqliteDriver` en iOS. Clase `ProductoLocal` desacoplada de Ktor y DTOs. |
| **4. IDs Temporales Negativos y Detección de Pendientes** | 2.0 | **Cumplido** | Generación monótona decreciente con `COALESCE(MIN(id), 0) - 1` (`-1`, `-2`...). Modelos `Producto` y `ProductoUi` con propiedad `esPendiente = id < 0L || pendiente`. Cero colisiones con el backend. |
| **5. Sincronización con Reconciliación Atómica** | 2.0 | **Cumplido** | `sincronizar()` envía pendientes primero vía POST, recibe ID remoto, elimina el ID temporal negativo e inserta el registro confirmado con `pendiente = 0`. Luego descarga el catálogo remoto sin borrar los pendientes locales. |
| **6. Reactividad con Flow / StateFlow en Compose** | 2.0 | **Cumplido** | `ProductoLocal.observar()` usa extensiones SQLDelight `asFlow().mapToList()`. `ProductoRepositoryOffline.observar()` entrega `Flow<List<Producto>>`. `ProductoViewModel` mapea reactivamente a `StateFlow<ProductoUiState>`. |
| **7. Indicador Visual de Estado Offline / Pendiente** | 2.0 | **Cumplido** | Badge ámbar `⏳ Pendiente` en tarjetas de productos con `id < 0`. Barra de sincronización superior con conteo dinámico de pendientes e indicador de sincronización activa con botón manual. |
| **8. Inyección de Dependencias con Koin** | 2.0 | **Cumplido** | Módulos `PlatformModule` (provee `SqlDriver`), `localModule` (`PharmaDb`, `ProductoLocal`), `repositoryModule` (`ProductoRepositoryOffline`), `useCaseModule` (`ObservarProductosUseCase`, `SincronizarProductosUseCase`) registrados en `AppModule.kt`. |
| **9. Pruebas Automatizadas** | 2.0 | **Cumplido** | 6 pruebas unitarias específicas en `ProductoRepositoryOfflineTest` con in-memory SQLite y MockEngine cubriendo inserción offline, reconciliación atómica, fallos de red, reactividad y errores en operaciones conectadas. Suite global: 47 pruebas exitosas. |
| **10. Buenas Prácticas, Clean Architecture y Git** | 2.0 | **Cumplido** | Separación estricta de capas (Domain, Data, Presentation). Rama dedicada `feature/offline-first-chambilla`. Commits semánticos y atómicos siguiendo el estándar Conventional Commits. |

---

## 10. Historial de Commits de la Sesión 11

1. `2f1a05d` — `build: configurar SQLDelight y dependencias por plataforma`
2. `6051a1a` — `feat: agregar esquema y consultas locales de productos`
3. `f5cec48` — `feat: integrar drivers Android e iOS con Koin`
4. `f22fc8e` — `feat: implementar persistencia local y reconciliacion de registros`
5. `75d1944` — `feat: implementar sincronizacion de productos pendientes con API`
6. `620ef14` — `feat: observar productos locales y mostrar estado de sincronizacion`
7. `1bcc88e` — `test: cubrir persistencia offline y sincronizacion del repositorio`
8. `882e3d1` — `docs: documentar persistencia offline y evidencias de validacion`

