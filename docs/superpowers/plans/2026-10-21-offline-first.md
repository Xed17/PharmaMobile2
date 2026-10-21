# Persistencia Offline-First con SQLDelight en PharmaMobile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the authenticated CRUD application (Session 10) into a robust offline-first mobile application (Session 11) using SQLDelight 2.1.0, reactive state with Kotlin Coroutines `Flow`, local storage with negative pending IDs (`id < 0`), atomic reconciliation upon server confirmation, and offline resilience.

**Architecture:**
- **Local Persistence:** SQLDelight (`PharmaDb`, package `pe.edu.upeu.pharmamobil.db`), table `producto` (5 fields: `id`, `nombre`, `precio`, `stock`, `pendiente`).
- **Platform Drivers:** `CreadorDeDriver` interface with implementations:
  - Android: `AndroidSqliteDriver` via `Context`
  - iOS: `NativeSqliteDriver`
  - JVM/Test: `JdbcSqliteDriver` in-memory
- **Local Layer:** `ProductoLocal` managing queries, negative ID assignment (`COALESCE(MIN(id), 0) - 1`), domain mapping, and atomic transactions.
- **Offline Repository:** `ProductoRepositoryOffline` exposing `observar(): Flow<List<Producto>>`, `sincronizar()` (upload pending before download, atomic reconciliation), and CRUD operations.
- **Reactive UI:** `ProductoViewModel` observing `Flow<List<Producto>>` with synchronization state and connectivity awareness. UI cards display amber `⏳ Pendiente` badge for unsynchronized products.

---

### Task 1: Preparar la rama Git

- [x] **Step 1:** Verificar que el árbol de trabajo esté limpio.
- [x] **Step 2:** Crear la rama `feature/offline-first-chambilla` a partir del estado actual (`fix/solucion-errores`).
  Command: `git switch -c feature/offline-first-chambilla`
- [x] **Step 3:** Confirmar que la rama está activa.
  Command: `git branch --show-current`

---

### Task 2: Configurar SQLDelight en Gradle

- [x] **Step 1:** Agregar la versión y librerías de SQLDelight en [libs.versions.toml](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/gradle/libs.versions.toml):
  - `sqldelight = "2.1.0"`
  - `sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }`
  - `sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }`
  - `sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }`
  - `sqldelight-coroutines-extensions = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }`
  - Plugin `sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }`
- [x] **Step 2:** Declarar el plugin en [build.gradle.kts](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/build.gradle.kts) raíz (`alias(libs.plugins.sqldelight) apply false`).
- [x] **Step 3:** Aplicar el plugin y configurar el bloque `sqldelight` en [shared/build.gradle.kts](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/shared/build.gradle.kts):
  ```kotlin
  sqldelight {
      databases {
          create("PharmaDb") {
              packageName.set("pe.edu.upeu.pharmamobil.db")
          }
      }
  }
  ```
- [x] **Step 4:** Agregar dependencias correspondientes a cada sourceSet:
  - `commonMain`: `sqldelight-coroutines-extensions`
  - `androidMain`: `sqldelight-android-driver`
  - `iosMain`: `sqldelight-native-driver`
  - `commonTest`: `sqldelight-sqlite-driver` (JDBC para pruebas JVM)
- [x] **Step 5:** Sincronizar Gradle y compilar para verificar la resolución de dependencias.
  Command: `./gradlew :shared:assemble`
- [x] **Step 6:** Commit atómico:
  `build: configurar SQLDelight y dependencias por plataforma`

---

### Task 3: Crear el esquema y las consultas SQLDelight

- [x] **Step 1:** Crear el archivo de esquema `Producto.sq` en:
  `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq`
- [x] **Step 2:** Definir la tabla con los 5 campos exigidos:
  ```sql
  CREATE TABLE producto (
      id INTEGER NOT NULL PRIMARY KEY,
      nombre TEXT NOT NULL,
      precio REAL NOT NULL,
      stock INTEGER NOT NULL,
      pendiente INTEGER NOT NULL DEFAULT 0
  );
  ```
- [x] **Step 3:** Implementar las consultas requeridas por la guía y la reconciliación:
  - `listarTodos: SELECT * FROM producto ORDER BY nombre ASC;`
  - `listarPendientes: SELECT * FROM producto WHERE pendiente = 1 ORDER BY id DESC;`
  - `siguienteIdLocal: SELECT COALESCE(MIN(id), 0) - 1 FROM producto WHERE id < 0;`
  - `insertar: INSERT OR REPLACE INTO producto(id, nombre, precio, stock, pendiente) VALUES (?, ?, ?, ?, ?);`
  - `borrarSincronizados: DELETE FROM producto WHERE pendiente = 0;`
  - `eliminarPorId: DELETE FROM producto WHERE id = ?;`
  - `contarPendientes: SELECT COUNT(*) FROM producto WHERE pendiente = 1;`
- [x] **Step 4:** Compilar para que SQLDelight genere la clase `PharmaDb` y los adapters.
  Command: `./gradlew :shared:generateCommonMainPharmaDbInterface`
- [x] **Step 5:** Verificar que `pe.edu.upeu.pharmamobil.db.PharmaDb` fue generado correctamente.
- [x] **Step 6:** Commit atómico:
  `feat: agregar esquema y consultas locales de productos`

---

### Task 4: Implementar drivers e inyección de dependencias

- [x] **Step 1:** Crear la interfaz común `CreadorDeDriver` en:
  `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/data/local/CreadorDeDriver.kt`
  ```kotlin
  interface CreadorDeDriver {
      fun crear(): app.cash.sqldelight.db.SqlDriver
  }
  ```
- [x] **Step 2:** Implementar `CreadorDeDriverAndroid` en:
  `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile2/data/local/CreadorDeDriverAndroid.kt`
  usando `AndroidSqliteDriver(PharmaDb.Schema, context, "pharma.db")`.
- [x] **Step 3:** Implementar `CreadorDeDriverIos` en:
  `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile2/data/local/CreadorDeDriverIos.kt`
  usando `NativeSqliteDriver(PharmaDb.Schema, "pharma.db")`.
- [x] **Step 4:** Registrar `CreadorDeDriver` en los módulos Koin de plataforma (`PlatformModule.android.kt` y `PlatformModule.ios.kt`).
- [x] **Step 5:** Registrar `PharmaDb` como `single` en el módulo común Koin (`KoinModules.kt`):
  `single { PharmaDb(get<CreadorDeDriver>().crear()) }`
- [x] **Step 6:** Compilar y verificar resolución de dependencias.
  Command: `./gradlew :shared:compileKotlinAndroid`
- [x] **Step 7:** Commit atómico:
  `feat: integrar drivers Android e iOS con Koin`

---

### Task 5: Implementar la capa `ProductoLocal`

- [x] **Step 1:** Crear la clase `ProductoLocal` en:
  `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/data/local/ProductoLocal.kt`
  Desacoplada al 100% de Ktor y de DTOs remotos.
- [x] **Step 2:** Implementar métodos reactivos y transaccionales:
  - `observar(): Flow<List<Producto>>`: utiliza `asFlow().mapToList(dispatcher)` para emitir la lista en tiempo real ante cualquier cambio local.
  - `pendientes(): List<Producto>`: retorna registros con `pendiente = 1`.
  - `guardarPendiente(producto: Producto): Producto`: calcula `siguienteIdLocal()` (ej: -1, -2...), inserta con `pendiente = 1` y retorna el `Producto` con su ID local negativo.
  - `confirmarRegistro(localId: Long, remoto: Producto)`: en una transacción `db.transaction`, ejecuta `eliminarPorId(localId)` e `insertar(...)` con los datos del servidor y `pendiente = 0`.
  - `reemplazarSincronizados(remotos: List<Producto>)`: en una transacción, ejecuta `borrarSincronizados()` e inserta los remotos con `pendiente = 0`, preservando intactos los registros pendientes.
  - `eliminarLocal(id: Long)`: elimina fila por id.
- [x] **Step 3:** Registrar `ProductoLocal` en Koin.
- [x] **Step 4:** Compilar para verificar contratos.
  Command: `./gradlew :shared:compileKotlinAndroid`
- [x] **Step 5:** Commit atómico:
  `feat: implementar persistencia local y reconciliacion de registros`

---

### Task 6: Implementar el repositorio offline `ProductoRepositoryOffline`

- [x] **Step 1:** Actualizar la interfaz [ProductoRepository](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/domain/repository/ProductoRepository.kt):
  - Agregar `fun observar(): Flow<List<Producto>>`
  - Agregar `suspend fun sincronizar()`
  - Mantener `registrar`, `actualizar`, `eliminar`, `obtener`.
- [x] **Step 2:** Crear la implementación [ProductoRepositoryOffline](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/data/repository/ProductoRepositoryOffline.kt):
  - `observar()`: delega directamente en `local.observar()`.
  - `registrar(producto)`: guarda primero localmente como pendiente con ID negativo (`local.guardarPendiente(producto)`). Luego dispara un intento de sincronización en segundo plano sin bloquear ni fallar si no hay red.
  - `sincronizar()`:
    1. Utiliza cerrojo/Mutex para evitar ejecuciones superpuestas.
    2. Sube pendientes primero: por cada pendiente con `id < 0`, ejecuta `api.crear(...)`, obtiene el `ProductoResponseDto` con ID confirmado y ejecuta `local.confirmarRegistro(pendiente.id, confirmado)`.
    3. Descarga sincronizados: consulta `api.obtenerProductos()`, los mapea a dominio y ejecuta `local.reemplazarSincronizados(remotos)`.
  - `actualizar` y `eliminar`: ejecutan la llamada remota a la API y luego actualizan la base local; ante error de red propagan `ErrorApi.SinConexion`.
- [x] **Step 3:** Reemplazar en Koin el repositorio por `ProductoRepositoryOffline`.
- [x] **Step 4:** Compilar y verificar.
  Command: `./gradlew :shared:compileKotlinAndroid`
- [x] **Step 5:** Commit atómico:
  `feat: implementar sincronizacion de productos pendientes con API`

---

### Task 7: Adaptar `ProductoViewModel` e Interfaz de Usuario

- [x] **Step 1:** Crear use case `ObservarProductosUseCase` y `SincronizarProductosUseCase` en `domain/usecase/`.
- [x] **Step 2:** Adaptar [ProductoViewModel](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/presentation/producto/ProductoViewModel.kt):
  - Iniciar observación continua en corrutina: `observarProductos().collect { lista -> _uiState.update { ... } }`.
  - Quitar recargas manuales imperativas `cargarProductos()` tras guardar.
  - Exponer estado de sincronización (`sincronizando: Boolean`, `mensajeSincronizacion: String?`).
  - Implementar acción `sincronizar()` llamada al abrir la pantalla o al presionar el botón de sincronización.
- [x] **Step 3:** Adaptar [ProductoUi.kt](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/presentation/producto/ProductoUi.kt):
  - Agregar propiedad `val esPendiente: Boolean = id < 0`.
- [x] **Step 4:** Adaptar pantalla de Productos [ProductoScreen.kt](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/presentation/producto/ProductoScreen.kt):
  - Mostrar badge ámbar `⏳ Pendiente` en cada tarjeta de producto con `id < 0`.
  - Agregar barra o chip superior con el estado de sincronización (e.g. `🔄 Sincronizando...`, `✅ Actualizado` o `☁️ 1 pendiente`) y botón de sincronizar manual.
- [x] **Step 5:** Compilar y verificar en Android.
  Command: `./gradlew :shared:compileKotlinAndroid`
- [x] **Step 6:** Commit atómico:
  `feat: observar productos locales y mostrar estado de sincronizacion`

---

### Task 8: Implementar la suite completa de 6 pruebas unitarias

- [x] **Step 1:** Implementar driver de pruebas para JVM en `shared/src/androidHostTest` o fábrica de pruebas compartida con `JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)`.
- [x] **Step 2:** Crear la suite de pruebas [ProductoRepositoryOfflineTest.kt](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobile2/ProductoRepositoryOfflineTest.kt):
  - **Test 1: Descarga con red:** Guarda productos remotos y emite en `observar()`.
  - **Test 2: Registro sin red:** Crea producto offline con ID negativo (`id < 0`), estado `pendiente = 1`, visible en `observar()`.
  - **Test 3: Reconexión exitosa:** Sube pendiente, elimina temporal negativo y guarda el producto remoto confirmado con ID positivo.
  - **Test 4: Segunda sincronización (Idempotencia):** No vuelve a enviar un registro ya confirmado.
  - **Test 5: Resiliencia (POST exitoso y GET fallido):** La confirmación del producto pendiente no se pierde si la descarga subsiguiente falla.
  - **Test 6: Reemplazo protegido:** La descarga y reemplazo de sincronizados no elimina pendientes locales no confirmados.
- [x] **Step 3:** Ejecutar la suite de pruebas unitarias y verificar que todas pasen al 100%.
  Command: `./gradlew :shared:testAndroidHostTest`
- [x] **Step 4:** Commit atómico:
  `test: cubrir persistencia offline y sincronizacion del repositorio`

---

### Task 9: Verificación de compilación global

- [x] **Step 1:** Ejecutar compilación de depuración de la aplicación Android.
  Command: `./gradlew :androidApp:assembleDebug`
- [x] **Step 2:** Confirmar `BUILD SUCCESSFUL`.
- [x] **Step 3:** Ejecutar todas las pruebas del proyecto.
  Command: `./gradlew test`

---

### Task 10: Documentación en README y Auditoría

- [x] **Step 1:** Agregar la sección «Persistencia offline-first» en [README.md](file:///c:/Users/USER/Documents/ciclo%206/moviles/PharmaMobile2/README.md) explicando arquitectura, esquema, asignación de IDs negativos, algoritmo de reconciliación y limitaciones conocidas.
- [x] **Step 2:** Verificar la matriz de cumplimiento de los 10 criterios de evaluación de la sesión 11.
- [x] **Step 3:** Commit atómico:
  `docs: documentar persistencia offline y evidencias de validacion`
