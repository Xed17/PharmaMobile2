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
