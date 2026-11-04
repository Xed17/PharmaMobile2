# Sesión 13 PharmaMobile2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Completar el entregable de sesión 13 en el móvil mediante sincronización recuperable, separación de cuentas, notificaciones locales y Firebase Crashlytics, con evidencia real Android y preparación iOS pendiente de verificar.

**Architecture:** Mantener Compose, Ktor, SQLDelight y Koin; capturar un contexto de cuenta inmutable por sesión y usarlo en todos los repositorios locales y solicitudes remotas. Separar catálogo, cola, descarga con backoff, permisos y telemetría mediante contratos de dominio. Reintentar automáticamente solo lecturas seguras; conservar las creaciones inciertas para revisión manual.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, coroutines/Flow, Ktor, Koin, SQLDelight 2.1.0, Android Activity Result/Notifications, iOS UserNotifications y GitLive Firebase Crashlytics.

**Spec:** `docs/superpowers/specs/2026-11-04-sesion13-integrador-design.md`, aprobado por el usuario. Diagnóstico y decisiones: `docs/sesion13/00-diagnostico-y-requisitos.md`.

## Global Constraints

- «Solo móvil; mantener backend intacto». No editar `../pharmasoft/pharmaSoft` ni crear migraciones del servidor.
- «Validar Android; preparar iOS con verificación pendiente». No afirmar compilación, pruebas ni evidencias iOS ejecutadas en Windows.
- «Notificaciones locales de sincronización y filas fallidas». No incorporar FCM, push, cámara ni ubicación.
- «Offline solo para usuarios previamente autenticados, con datos separados por cuenta». No conservar el administrador ficticio ni mezclar borradores, colas o tareas entre usuarios.
- «Firebase Crashlytics, encapsulado detrás de Telemetria; no añadir Analytics ni Messaging».
- «Conservar los datos anteriores sin propietario en cuarentena local, sin adjudicarlos al siguiente usuario que entre».
- «Mantener actualización y eliminación remotas como operaciones en línea».
- Cinco recorridos completos como máximo por cadena GET; esperas entre fallos de 1, 2, 4 y 8 s; función limitada a 16 s.
- Tras agotar GET, acción explícita «Reintentar descarga» habilitada después de 16 s; no reiniciar presupuesto desde apertura, conectividad o botón general.
- Ningún reenvío automático de POST incierto; cinco intentos iniciados por operación antes de exigir rehabilitación manual.
- Mantener `applicationId = pe.edu.upeu.pharmamobile2`, base `pharma.db` y paquete generado `pe.edu.upeu.pharmamobil.db`.
- Conservar versiones actuales salvo incompatibilidad demostrada; no modernizar dependencias ajenas al objetivo.
- Clientes/pedidos: auditar, señalar demostración, limpiar estado entre cuentas y planificar REST para sesión 14.
- Capturas, reportes y estados «cumple» requieren evidencia; una prueba falsa no acredita Firebase ni un diálogo real.
- No tocar `docs/superpowers/plans/extracted_images/` ni incluir cambios ajenos en commits.

## Review Focus

1. Una respuesta A llega cuando ya inició sesión B: ni token B en la solicitud A, ni escritura/aviso A visible en B. Pruebas T05 y T10.
2. El servidor repite una página, devuelve IDs duplicados o `ultima=false` sin progreso: abortar sin borrar caché ni entrar en un bucle. Prueba T05.
3. El reloj retrocede o reinicia el proceso durante una espera: no renovar presupuesto, adelantar autenticación ni bloquear eternamente la UI. Pruebas T02 y T08.
4. Deslizar el diálogo de notificaciones no cambia el permiso: no convertir cancelación en denegación definitiva. Prueba T12 y evidencia E12.
5. Una excepción contiene token/correo en mensaje y causa: la telemetría no envía ninguno aunque su contexto esté filtrado. Prueba T15.

---

## 1. Cómo ejecutar y qué significa terminar

Este documento es el plan, no un informe de ejecución. Todas las casillas comienzan pendientes. No se ha ejecutado una batería nueva de tests durante su redacción.

Estado Git observado al terminar la redacción: `.gitignore` contiene una modificación ajena que añade `docs/`; este plan, el diseño y el diagnóstico existen localmente, pero quedan ignorados por esa regla. No modificar ni revertir esa decisión silenciosamente. Al ejecutar, antes del primer commit documental, resolver si esos archivos de entrega se incluirán selectivamente o se mantendrán como entrega local. Hasta entonces no afirmar que los documentos están versionados ni que se cumplió el commit del paso 1. La regla no impide trabajar en el código del móvil.

Implementar en el orden de dependencias; por tarea funcional: escribir pruebas, comprobar un fallo relacionado, implementar el mínimo y ejecutar la prueba específica. Al cerrar cada etapa ejecutar el conjunto pertinente. No perseguir fallos previos como si los causara una tarea nueva; registrarlos en la línea base.

Las tareas se agrupan en los siete pasos de la guía. Constituyen un único proyecto porque comparten identidad, esquema y ciclo de vida; no son tres aplicaciones independientes. Permisos y telemetría tienen contratos separados para que sus pruebas no necesiten servidor.

### Rutas exactas usadas en el plan

Para evitar repetir el paquete en cada archivo, estas abreviaturas se expanden literalmente desde la raíz `C:/Users/USER/Documents/ciclo 6/moviles/PharmaMobile2`; no son variables de shell:

| Prefijo | Expansión |
| --- | --- |
| `C/` | `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/` |
| `A/` | `shared/src/androidMain/kotlin/pe/edu/upeu/pharmamobile2/` |
| `I/` | `shared/src/iosMain/kotlin/pe/edu/upeu/pharmamobile2/` |
| `T/` | `shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobile2/` |
| `H/` | `shared/src/androidHostTest/kotlin/pe/edu/upeu/pharmamobile2/` |
| `APP/` | `androidApp/src/main/kotlin/pe/edu/upeu/pharmamobile2/` |
| `AT/` | `androidApp/src/androidTest/kotlin/pe/edu/upeu/pharmamobile2/` |
| `SQL/` | `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/` |

### Mapa de componentes y archivos

| Grupo | Archivos a crear | Archivos existentes a modificar | Responsabilidad |
| --- | --- | --- | --- |
| Tipos | `C/domain/model/ContextoCuenta.kt`, `PerfilLocal.kt`, `AccesoSesion.kt`, `EstadoEnvio.kt`, `RegistroEnvio.kt`, `FalloOperacion.kt`, `ResultadoEnvio.kt`, `CadenaDescarga.kt`, `ResumenSincronizacion.kt`, `BorradorProducto.kt` | `C/domain/model/Sesion.kt` si necesita fecha absoluta de expiración | Tipos sin dependencias de SDK |
| Puertos | `C/domain/port/Reloj.kt`, `Espera.kt`, `GeneradorIds.kt`, `Telemetria.kt`, `GestorPermisos.kt`, `NotificadorSincronizacion.kt`, `MonitorConectividad.kt` | — | Tiempo, plataforma y proveedor sustituibles |
| Sesión | `C/data/local/PerfilSeguro.kt`, `C/di/ScopesCuenta.kt` | `C/domain/repository/AuthRepository.kt`, `C/data/repository/AuthRepositorioRest.kt`, almacenamientos seguros Android/iOS, `C/presentation/sesion/*`, `C/App.kt` | Recuperación offline y ciclo de cuenta |
| Esquema | `SQL/1.sqm`, `BorradorProducto.sq`, `Descarga.sq`, `HistorialEnvio.sq`; snapshot de esquema 1 | `SQL/Producto.sq`, `Metadato.sq`, `shared/build.gradle.kts` | Migración y separación por cuenta |
| Local | `C/data/local/ColaProductoLocal.kt`, `BorradorProductoLocal.kt`, `DescargaLocal.kt`, `C/data/platform/DependenciasTiempo.kt`, `A/data/local/GeneradorIdsAndroid.kt`, `I/data/local/GeneradorIdsIos.kt` | `C/data/local/ProductoLocal.kt` | Transacciones, observación y dependencias reales de tiempo/IDs |
| Contratos de repositorio | `C/domain/repository/ColaProductoRepository.kt`, `BorradorProductoRepository.kt`, `DescargaRepository.kt`, `ProductoRemotoRepository.kt` | `C/domain/repository/ProductoRepository.kt` conserva CRUD orientado a cuenta vinculada | Fronteras de negocio |
| Remoto | `C/data/remote/AutorizadorSolicitudes.kt`, `C/data/repository/ProductoRemotoRest.kt` | `ProductoApi.kt`, `EjecutarLlamada.kt`, `HttpClientFactory.kt`, `ProductoRepositorioRest.kt` | Token capturado, paginación y errores |
| Sincronización | `C/domain/usecase/DescargarCatalogoUseCase.kt`, `RevisarEnvioUseCase.kt`, `C/domain/sync/PoliticaReintento.kt`, `C/data/sync/CoordinadorSincronizacion.kt` | `SincronizarProductosUseCase.kt`, `ProductoRepositoryOffline.kt`, `RegistrarProductoUseCase.kt`, `C/di/AppModule.kt` | Primer envío, backoff, revisión y disparadores |
| UI | `C/presentation/sincronizacion/SincronizacionUiState.kt`, `SincronizacionViewModel.kt`, `ResumenSincronizacionCard.kt`, `RevisionEnviosScreen.kt` | Pantallas/ViewModels de producto, detalle, Inicio, sesión; `Screen.kt`, `App.kt` | Estado observable y acciones |
| Android permisos | `A/data/permiso/GestorPermisosAndroid.kt`, `PuenteSolicitudPermiso.kt`; `APP/permiso/PuentePermisosActivity.kt` | `APP/MainActivity.kt`, `A/di/PlatformModule.android.kt`, manifiesto | Lifecycle y tres estados |
| Android avisos | `A/data/notificacion/NotificadorAndroid.kt`, `A/data/conectividad/MonitorConectividadAndroid.kt`, `C/data/notificacion/PoliticaAvisos.kt` | MainActivity, recursos de icono, plataforma Koin | Canal, tap y avisos sin datos personales |
| iOS | `I/data/permiso/GestorPermisosIos.kt`, `I/data/notificacion/NotificadorIos.kt`, `I/data/conectividad/MonitorConectividadIos.kt` | `I/di/PlatformModule.ios.kt`, `iosApp/iosApp/iOSApp.swift`, proyecto Xcode | Preparación nativa por verificar |
| Firebase | `C/data/firebase/TelemetriaFirebase.kt`, `TelemetriaSegura.kt` | Catálogo de versiones, Gradle raíz/app/shared, Application, proyecto iOS | Adaptador y configuración |
| Pruebas | Clases descritas en cada tarea; `T/testutil/FixtureLocal.kt`, `ServidorSimulado.kt`, `EscenarioSync.kt`, `DoblesSesion13.kt` | Pruebas existentes afectadas y drivers de pruebas | SQL real en memoria, HTTP simulado y tiempo virtual |
| Entrega | `docs/sesion13/01-recorrido-y-huecos.md`, `02-configuracion.md`, `03-matriz-evidencias.md`, `04-informe.md`, `05-backlog-sesion14.md`, `06-verificacion-ios-pendiente.md`, `evidencias/`, `logs/` | README | Ejecución y trazabilidad |

No introducir archivos vacíos para completar esta lista. Cada archivo se crea con la tarea que necesita su responsabilidad; combinar un tipo muy pequeño con su familia si no cambia el contrato de este plan.

## 2. Contratos comunes que no deben divergir

### 2.1 Modelos y valores

En `domain/model`:

```kotlin
data class ContextoCuenta(val clave: String, val generacion: Long)
enum class ModoAcceso { REMOTO, LOCAL }
enum class EstadoEnvio {
    PENDIENTE, ENVIANDO, CONFIRMADO, REVISION,
    FALLIDA_PERMANENTE, FALLIDA_AGOTADA, REQUIERE_SESION, REQUIERE_PERMISO
}
enum class NaturalezaFallo { TRANSITORIO, PERMANENTE, SESION, PERMISOS }
data class FalloOperacion(
    val naturaleza: NaturalezaFallo,
    val codigo: String,
    val mensajeSeguro: String,
    val campos: Map<String, String> = emptyMap(),
    val efectoIncierto: Boolean = false
)
sealed interface ResultadoEnvio {
    data class Exito(val producto: Producto) : ResultadoEnvio
    data class Fallo(val error: FalloOperacion) : ResultadoEnvio
}
```

`PerfilLocal(username: String, nombreCompleto: String, roles: List<String>, permisos: Set<String>, validadoEnMs: Long, expiraTokenEnMs: Long?)` no contiene contraseña ni token. `AccesoSesion` tiene `SinSesion`, `Disponible(contexto: ContextoCuenta, perfil: PerfilLocal, modo: ModoAcceso)` y `Reautenticar(contexto: ContextoCuenta, perfil: PerfilLocal, motivo: String)`; no convertir un modo local en remoto por restaurar un perfil.

`RegistroEnvio(producto: Producto, estado: EstadoEnvio, operacionLocalId: String, revision: Int, intentos: Int, ultimoError: FalloOperacion?, proximoIntentoEnMs: Long?)` conserva producto y metadatos de envío sin filtrar SDK. `BorradorProducto(idEdicion: Long?, nombre: String, precioTexto: String, stockTexto: String, categoriaId: Long?, categoriaNombre: String?)` conserva textos incompletos sin inventar números válidos.

`EstadoCadena = NUEVA | EN_CURSO | ESPERA | AGOTADA | BLOQUEADA | COMPLETA`. `CadenaDescarga(id: String, estado: EstadoCadena, intentos: Int, proximoEnMs: Long?, ultimoFallo: FalloOperacion?)` es por cuenta y persiste antes del primer GET. `ResumenSincronizacion(enviadas: Int, pendientes: Int, fallidas: Int, revision: Int, bloqueadas: Int, descargaCompleta: Boolean, ultimaSync: Long?)`; `fallidas` suma permanente y agotada, `bloqueadas` suma sesión y privilegios. `pendientes` cuenta solo PENDIENTE y ENVIANDO; en UI se muestran los demás contadores por separado.

### 2.2 Puertos y ámbito

Todos los repositorios de catálogo, cola, borrador y descarga se construyen con `ContextoCuenta` inmutable. Sus métodos no reciben una cuenta opcional ni usan «la cuenta actual» mutable durante una escritura. Al abrir una sesión se crea un scope nuevo; al cerrarla se destruye después de cancelar sus tareas.

```kotlin
interface Reloj { fun ahoraMs(): Long }
interface Espera { suspend fun esperar(ms: Long) }
interface GeneradorIds { fun nuevo(): String }

interface ColaProductoRepository {
    fun observar(): Flow<List<RegistroEnvio>>
    suspend fun elegibles(): List<RegistroEnvio>
    suspend fun porId(id: Long): RegistroEnvio?
    suspend fun guardar(producto: Producto): RegistroEnvio
    suspend fun iniciar(id: Long, ahoraMs: Long): RegistroEnvio?
    suspend fun resolver(id: Long, resultado: ResultadoEnvio, ahoraMs: Long)
    suspend fun recuperarInterrumpidos()
    suspend fun autorizarReenvio(id: Long, ahoraMs: Long, aceptaRiesgo: Boolean)
    suspend fun corregirRechazado(id: Long, producto: Producto)
    suspend fun rehabilitarAgotado(id: Long, ahoraMs: Long, aceptaRiesgo: Boolean)
    suspend fun descartarLocal(id: Long)
}
interface BorradorProductoRepository {
    fun observar(): Flow<BorradorProducto?>
    suspend fun guardar(borrador: BorradorProducto)
    suspend fun borrar()
}
interface DescargaRepository {
    suspend fun actual(): CadenaDescarga?
    suspend fun guardar(cadena: CadenaDescarga)
    suspend fun conciliarCompleta(productos: List<Producto>, ahoraMs: Long)
    fun observarUltimaSync(): Flow<Long?>
}
interface ProductoRemotoRepository {
    suspend fun crear(producto: Producto): ResultadoEnvio
    suspend fun listarCompleto(): Result<List<Producto>>
}
```

`ProductoRepository` conserva `observar`, `listar`, `obtener`, `registrar`, `actualizar`, `eliminar` con las firmas existentes, pero ligado al scope de cuenta. Se retira `sincronizar()` de ese contrato cuando T07-T09 migren sus consumidores al coordinador. Se retiran simulaciones de antigüedad del contrato normal en T11. El repositorio REST antiguo deja de ser una segunda implementación activa de catálogo: T05 lo transforma en adaptador remoto o lo elimina al reemplazar sus usos, sin dos fuentes de verdad.

`AuthRepository` añade `acceso: StateFlow<AccesoSesion>`, `suspend fun sesionRemota(contexto: ContextoCuenta): Sesion?`, `fun esActual(contexto: ContextoCuenta): Boolean`, `suspend fun habilitarContinuidadLocal(): Result<ContextoCuenta>`. Mantiene login, recuperación, salida y eventos 401/403; `sesionActual` solo puede derivarse de la misma fuente de estado mientras haya consumidores antiguos y se retira al migrarlos. Una sesión remota se entrega solo si cuenta/generación coinciden, token no vencido y no hubo rechazo 401.

La clave de cuenta usa el namespace estable de entorno `pharmasoft-desarrollo-local` y el username canónico del servidor, codificados con longitud para evitar colisiones. No hacer depender el namespace de `10.0.2.2` frente a la IP LAN del mismo backend; otro servidor real requiere otro namespace. No pasar a minúsculas el username sin garantía del backend.

## 3. Etapas, dependencias y commits

| Paso guía | Tareas | Entrega comprobable | Commit de cierre propuesto |
| --- | --- | --- | --- |
| 1. Recorrer y priorizar | T00 | Línea base y ocho recorridos documentados | `docs(s13): registrar recorrido y brechas del integrador` |
| 2. Ampliar base | T01-T04 | Contexto, sesión local y migración con intentos/errores/cuentas | `feat(local): persistir estado de envio por cuenta` |
| 3. Refinar sync | T05-T10 | Paginación, resultados, cola, backoff y recuperación segura | `feat(sync): refinar envios y reintentos seguros` |
| 4. Mostrar estado | T11 | Resumen, revisión y borrador observable en UI | `feat(ui): mostrar estado y recuperacion de sincronizacion` |
| 5. Permisos | T12-T14 | Permisos y avisos Android; preparación iOS | `feat(native): integrar permisos y avisos locales` |
| 6. Firebase | T15-T16 | Crashlytics real Android; configuración iOS preparada | `feat(telemetry): integrar Crashlytics mediante contrato` |
| 7. Probar y evidenciar | T17-T18 | Regresión, evidencias, informe y backlog | `test(s13): verificar integrador y documentar evidencias` |

Dependencia principal: T00 -> T01 -> T02 -> T03 -> T04 -> T05 -> T06 -> T07 -> T08 -> T09 -> T10 -> T11 -> T12 -> T13 -> T14 -> T15 -> T16 -> T17 -> T18. Los puertos permiten pruebas aisladas; el orden de integración y commits se conserva para la guía. Cada tarea termina revisando su diff; cada paso de guía tiene al menos su commit. Se permiten commits adicionales de subtareas completas, sin mover todo a un único commit final.

En cada commit: añadir solo archivos de las tareas terminadas, comprobar `git diff --cached --name-only`, no incluir archivos ya staged por el usuario, ejecutar la prueba del paso y usar el mensaje propuesto. No usar `git add .` ni reescribir commits existentes.

## 4. Tareas ejecutables

### Task 0: T00 — Preparar ejecución, línea base y recorrido de ocho escenarios

**Depende de:** diseño aprobado. **Archivos:** crear `docs/sesion13/01-recorrido-y-huecos.md`, `02-configuracion.md`, `logs/`; modificar README solo para enlazar el trabajo.

**Consume:** repositorios existentes. **Produce:** inventario de tareas Gradle, versiones, estado inicial, cuentas/entorno de prueba sin secretos y tabla de ocho escenarios con gravedad, decisión y evidencia.

- [ ] Registrar `git status --short`, rama y HEAD. Si ya existe worktree aislado, usarlo; si se necesita aislamiento, seguir la skill `using-git-worktrees` al ejecutar. Crear o reutilizar `feature/integrador-13` desde el `develop` comprobado, sin descartar archivos ajenos ni resetear ramas existentes. Si el trabajo se traslada a un worktree, copiar únicamente plan/diseño/diagnóstico propios, pues hoy están sin seguimiento e ignorados; no mover ni stashear cambios del usuario.
- [ ] Ejecutar, cada comando por separado: `java -version`; `.\gradlew.bat --version`; `.\gradlew.bat :shared:tasks --all`; `.\gradlew.bat :androidApp:tasks --all`. Guardar logs y nombres reales de tareas SQL/host/device. La configuración actual declara JDK 21 y wrapper Gradle 9.1.0; no afirmar disponibilidad de herramientas sin comprobarlas.
- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest` y `.\gradlew.bat :androidApp:assembleDebug`. Esperado: `BUILD SUCCESSFUL`, reporte de tests sin fallos y APK debug. Si falla, registrar causa/log y resolver solo lo que impida la línea base antes de atribuir fallos a sesión 13.
- [ ] Comprobar backend y dispositivo con operaciones de lectura; ejecutar `adb devices` usando el SDK encontrado si adb no está en PATH. Registrar emulador/API, dispositivo físico si lo hay y URL de backend por entorno. No imprimir configuraciones con credenciales.
- [ ] Ejecutar los ocho recorridos de diagnóstico: primera apertura; login incorrecto; local vacío; alta offline; reconexión; validación; expiración en formulario; salida/cambio de cuenta. Usar datos de prueba identificables y mantener aparte un registro de IDs creados. No borrar datos de un dispositivo ajeno para simular instalación limpia: utilizar un AVD o perfil de prueba.
- [ ] Documentar lo observado, no lo esperado, con columnas `ID`, preparación, pasos, resultado, gravedad, decisión hoy/S14/descartar, prueba futura y evidencia. Si falta dispositivo, registrar «pendiente» y preparar el guion sin inventar capturas.
- [ ] Cerrar paso 1 con el commit de su fila. **Cierre:** línea base y todos los huecos tienen decisión explícita; no se han cambiado endpoints ni datos de configuración del backend.

### Task 1: T01 — Definir modelos, puertos y política pura de reintento

**Depende de:** T00. **Archivos:** modelos de sección 2.1; puertos `C/domain/port/Reloj.kt`, `Espera.kt`, `GeneradorIds.kt`; `C/domain/sync/PoliticaReintento.kt`; crear `T/PoliticaReintentoTest.kt`, `T/EstadosEnvioTest.kt`, `T/testutil/DoblesSesion13.kt`. Los demás puertos/repositorios se crean en sus tareas propietarias; no modificar todavía el contrato CRUD existente.

**Consume:** `Producto`. **Produce:** modelos de sección 2.1, puertos Reloj/Espera/GeneradorIds y `PoliticaReintento.esperaMs(fallosPrevios: Int): Long`, `puedeReintentarAutomaticamente(error: FalloOperacion, esLectura: Boolean): Boolean`, `const val MAX_INTENTOS = 5`.

- [ ] Escribir las pruebas con estas aserciones y fixtures literales:

```kotlin
assertEquals(listOf(1000L, 2000L, 4000L, 8000L, 16000L), (0..4).map(politica::esperaMs))
assertEquals(16000L, politica.esperaMs(100))
assertEquals(5, PoliticaReintento.MAX_INTENTOS)
assertFalse(politica.puedeReintentarAutomaticamente(transitorioIncierto, esLectura = false))
assertTrue(politica.puedeReintentarAutomaticamente(transitorioLectura, esLectura = true))
```

`transitorioIncierto` y `transitorioLectura` son `FalloOperacion(TRANSITORIO, "TIMEOUT", "Tiempo agotado", efectoIncierto=true/false)` definidos en el test. Añadir `intentoNoPuedeVolverDeRevisionAPendienteSinAccion` y `agotadaNoEsUnaFilaConfirmada` sobre la política de transición.
- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest --tests "pe.edu.upeu.pharmamobile2.PoliticaReintentoTest"`; esperar fallo relacionado con tipos/política aún ausentes, no con entorno.
- [ ] Implementar tipos y política; rechazar contadores negativos; limitar desplazamiento antes de calcular para evitar overflow. No incluir Ktor, Firebase, Android ni UIKit.
- [ ] Crear dobles `RelojFalso(var instanteMs: Long)`, `EsperaFalsa(val demoras: MutableList<Long>)` y `GeneradorIdsSecuencial`. `EsperaFalsa` usa `delay` en el scheduler de `runTest`, no sleep real. TelemetriaFalsa se añadirá al necesitarla en T15.
- [ ] Ejecutar las dos clases nuevas. **Cierre:** valores exactos y transición incierta comprobados; ningún puerto necesita servidor.

### Task 2: T02 — Identidad validada, continuidad offline y scope de cuenta

**Depende de:** T01. **Modificar:** `C/domain/repository/AuthRepository.kt`, `C/data/repository/AuthRepositorioRest.kt`, `C/data/local/AlmacenSeguro.kt`, adaptadores seguros A/I, sesión UI/ViewModel, `C/App.kt`, `C/di/AppModule.kt`. **Crear:** `PerfilSeguro.kt`, `ScopesCuenta.kt`; `T/ContinuidadSesionTest.kt`. Actualizar pruebas auth y sesión existentes.

**Consume:** `ContextoCuenta`, `PerfilLocal`, `AccesoSesion`, `Reloj`. **Produce:** `AuthRepository` ampliado en sección 2.2; `ScopesCuenta.abrir(contexto: ContextoCuenta)`, `suspend cerrar(contexto: ContextoCuenta)` y `AlmacenSeguro.guardarPerfil(perfil: PerfilLocal, continuidad: Boolean)`, `obtenerPerfil(): PerfilLocal?`, `borrarContinuidad()` además del token actual.

- [ ] Escribir `primeraAperturaSinRedNoCreaAdministrador`, `perfilValidadoPermiteModoLocalSinTokenRemoto`, `salidaImpideReaperturaOffline` y `relojAnteriorALoginNoExtiendeToken`:

```kotlin
assertIs<AccesoSesion.SinSesion>(sinPerfil.acceso.value)
assertEquals(ModoAcceso.LOCAL, (restaurado.acceso.value as AccesoSesion.Disponible).modo)
assertNull(restaurado.sesionRemota(contextoRestaurado))
assertFalse(restaurado.esActual(contextoDeGeneracionAnterior))
assertNull(authConRelojRetrocedido.sesionRemota(contextoActual))
```

Preparación: almacenamientos falsos independientes, perfil recibido tras login simulado y reloj a `validadoEnMs - 1`; no decodificar un JWT como prueba de autenticación.
- [ ] Ejecutar la clase nueva y `AuthRepositorioRestTest`/`SesionViewModelTest`; registrar el fallo específico antes del cambio.
- [ ] Persistir perfil solo tras respuesta autenticada válida. Calcular expiración absoluta con el TTL de login; `/me` no reinicia ese TTL a otra hora. Si solo existe el token antiguo sin perfil, exigir recuperación online antes de habilitar continuidad.
- [ ] Implementar modos y scopes: login/recuperación cambian generación; clave de entorno+username sin colisiones; 401 bloquea remoto; modo local solo con continuidad previamente validada. Eliminar `accederModoOffline()` que fabrica administrador; sustituir su botón por «Continuar sin conexión» únicamente cuando hay perfil elegible.
- [ ] Quitar fallback silencioso a preferencias planas en Android. Un fallo de almacenamiento devuelve error recuperable sin guardar contraseña ni presentar sesión como persistida. En iOS comprobar códigos Keychain y preparar pruebas posteriores; no certificar ejecución.
- [ ] Adaptar `App` a perfil/contexto y resolver ViewModels dentro del scope. La infraestructura HTTP/DB puede ser global; repositorios de productos y ViewModels pasan a ámbito de cuenta cuando T03 les añada el contexto. Mantener UI compilable al finalizar esta tarea; no añadir stubs exitosos para el modo offline.
- [ ] Ejecutar las tres clases afectadas y una apertura online/offline. **Cierre:** identidad y modo correctos; la separación de filas se acredita en T03, no se declara terminada aquí.

### Task 3: T03 — Migrar producto y metadatos sin perder datos ni atribuir propietarios

**Depende de:** T02. **Modificar:** `SQL/Producto.sq`, `Metadato.sq`, `C/data/local/ProductoLocal.kt`, fábricas Koin y constructores de pruebas existentes. **Crear:** `SQL/1.sqm`, snapshot `SQL/1.db`, `SQL/BorradorProducto.sq`, `SQL/Descarga.sq`, `SQL/HistorialEnvio.sq`, `H/MigracionSesion13Test.kt`. Configurar `schemaOutputDirectory` en shared Gradle para verificar snapshots.

**Consume:** contexto del scope y esquema v1. **Produce:** `ProductoLocal(db: PharmaDb, contexto: ContextoCuenta, dispatcher: CoroutineDispatcher)` con la API de catálogo existente filtrada por cuenta y `suspend fun listar(): List<Producto>` como lectura puntual; esquema v2 con PK `(cuentaClave, id)` y metadatos por cuenta.

- [ ] Crear una base v1 en memoria a partir de `1.db`, insertar un producto confirmado y otro negativo con valores conocidos y metadato de fecha. Escribir `migracionPreservaFilasSinAsignarlasAUsuario`, `dosCuentasPuedenTenerMismoId` y `instalacionNuevaEquivaleAMigrada`:

```kotlin
assertEquals(2L, contarFilasDe("legacy_unassigned"))
assertEquals(0, localA.listar().size)
assertEquals(0, localB.listar().size)
assertEquals("Producto previo", leerLegado(-1).nombre)
assertEquals(esquemaEsperadoNuevo, esquemaMigrado)
```

Las funciones de inspección del test ejecutan SQL por driver; no forman parte del repositorio de UI.
- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest --tests "pe.edu.upeu.pharmamobile2.MigracionSesion13Test"`; esperar fallo de esquema/filtrado.
- [ ] Configurar salida de snapshots antes de alterar `.sq`, volver a consultar `.\gradlew.bat :shared:tasks --all` y registrar el nombre real de generación/verificación SQL disponible tras esa configuración. Capturar snapshot v1; reconstruir tabla en `1.sqm`, copiar filas a `legacy_unassigned` y recrear índices. Si T00 descubre una versión distinta de 1, usar `<versión_actual>.sqm` y snapshot real, sin sobrescribir una migración existente.
- [ ] Añadir los campos de sección 7 del diseño y `revision INTEGER NOT NULL DEFAULT 0`. Conservar los valores de datos antiguos; `categoriaId` y estado no conocidos quedan identificados como incompletos y no elegibles. En filas nuevas/remotas conservar categoría y activo reales. No inferir que el ID de categoría 26 vale para todos los productos.
- [ ] Filtrar en TODAS las consultas: listar, obtener, pendientes, borrar sincronizados, confirmar, siguiente ID, contador y metadatos. Asignar ID negativo e insertar dentro de una transacción. Mantener consultas compatibles con las firmas públicas actuales de `ProductoLocal`, adaptando sus consumidores y drivers de pruebas en la misma tarea.
- [ ] Ejecutar la prueba de migración y las existentes de repositorio. Ejecutar también la tarea SQL de verificación descubierta en T00; nombre esperado según SQLDelight, pero usar el publicado por este proyecto, no uno supuesto. **Cierre:** snapshot anterior preservado, esquema equivalente y cuentas aisladas incluso con el mismo ID.

### Task 4: T04 — Persistir estados de envío, cadenas de descarga y borradores

**Depende de:** T03. **Crear:** `C/data/local/ColaProductoLocal.kt`, `BorradorProductoLocal.kt`, `DescargaLocal.kt`, sus contratos y `T/ColaProductoLocalTest.kt`, `T/BorradorProductoTest.kt`, `T/testutil/FixtureLocal.kt`; dependencias reales de tiempo e IDs indicadas en el mapa. **Modificar:** consultas SQL correspondientes, incluido `SQL/HistorialEnvio.sq`, y Koin de cuenta.

**Consume:** DB v2, contexto, reloj e IDs. **Produce:** implementaciones de los tres contratos de sección 2.2. `FixtureLocal` expone `ctxA`, `ctxB`, `localA/localB`, `colaA/colaB`, `borradorA/borradorB`, `descargaA/descargaB` y `db`, usando driver real de pruebas; cada fixture cierra su driver.

- [ ] Escribir `iniciarEsAtomicoYCuentaUnSoloIntento`, `reinicioConvierteEnviandoEnRevision`, `rechazoPermanenteNoEsElegible` y `borradorNoSeCruzaEntreCuentas`:

```kotlin
assertEquals(1, listOfNotNull(primerInicio, segundoInicio).size)
assertEquals(1, colaA.porId(id)!!.intentos)
assertEquals(EstadoEnvio.REVISION, colaTrasReinicio.porId(id)!!.estado)
assertNull(borradorB.observar().first())
assertEquals("12.", borradorA.observar().first()!!.precioTexto)
```

- [ ] Ejecutar las clases nuevas y observar fallo de ausencia de transición/persistencia.
- [ ] Implementar `iniciar` con compare-and-set SQL: solo PENDIENTE, contador `<5`, cuenta coincidente y tiempo elegible; modificar estado y contador en la misma transacción. `resolver` confirma transaccionalmente o guarda error seguro/campos, sin borrar por fallar.
- [ ] Implementar recuperación de ENVIANDO, consultas observables por estado, borradores de texto y fila de descarga por cuenta. Mantener `pendiente` consistente: 0 solo CONFIRMADO; 1 en el resto. No emplear `INSERT OR REPLACE` de modo que reinicie intentos por una modificación de presentación.
- [ ] Implementar corrección y revisión: una fila incierta no cambia payload; un rechazo definitivo puede corregirse creando nueva revisión lógica con intentos 0 y conservando su último rechazo para UI. Guardar el historial técnico por operación/revisión/intento en `historial_envio`, sin datos personales ni cuerpos HTTP. Antes de descartar capturar la confirmación en el caso de uso, no borrar remoto. Rehabilitar agotada exige acción explícita y se registra como revisión nueva.
- [ ] Implementar `RelojSistema` mediante `obtenerTiempoActualMs()` existente, `EsperaCoroutines` mediante `delay`, y generadores UUID nativos Android/iOS en datos. Registrarlos en Koin. No usar IDs secuenciales de pruebas como proveedor real ni lanzar GlobalScope.
- [ ] Ejecutar unitarias locales, regresión de repositorio y migración. **Cierre:** transiciones persistidas y sin cruces. Cerrar paso 2 con su commit después de comprobar que las pruebas modificadas compilan y pasan.

### Task 5: T05 — Capturar credenciales, clasificar fallos y descargar todas las páginas

**Depende de:** T04. **Crear:** `C/domain/repository/ProductoRemotoRepository.kt`, `C/data/remote/AutorizadorSolicitudes.kt`, `C/data/repository/ProductoRemotoRest.kt`, `T/ProductoRemotoSesion13Test.kt`, `T/testutil/ServidorSimulado.kt`. **Modificar:** `ProductoApi.kt`, `EjecutarLlamada.kt`, `HttpClientFactory.kt`, `ProductoRepositorioRest.kt` y pruebas remotas existentes.

**Consume:** `AuthRepository.sesionRemota(contexto)`, contexto del scope, `ProductoApi`. **Produce:** `ProductoRemotoRepository` de sección 2; `AutorizadorSolicitudes.conCredencial(contexto, bloque: suspend (token: String) -> T): T`; `FalloOperacionException(val fallo: FalloOperacion)` como excepción de dominio para lecturas. `ProductoApi` recibe token explícito por solicitud privada y ningún token en login; el helper de cabecera vive solo en datos.

Fixture `ServidorSimulado`: `getPaginas: MutableMap<Int, RespuestaSimulada>`, `modoPost: ModoPost`, `postsIniciados: Int`, `creados: Int`, `paginasPedidas: List<Int>`, `tokensRecibidos: List<String?>`; modos POST `EXITO`, `VALIDACION`, `CONFLICTO`, `SIN_PERMISO`, `EXPIRADA`, `APLICADA_SIN_RESPUESTA`, `SERVIDOR`, `JSON_INVALIDO`. Las respuestas son DTO JSON del contrato real y las peticiones se ejecutan mediante Ktor MockEngine, no sustituyendo la capa remota por un contador vacío.

- [ ] Escribir `listaMasDe20SinPerderCategoriaNiActivo`, `paginaSinProgresoAborta`, `idsRepetidosNoSeConcilian`, `postAplicadoSinRespuestaEsIncierto`, `tokenNoCambiaEntreCapturaYEnvio` y `serializacionNoSePresentaComoSinRed`:

```kotlin
assertEquals(listOf(0, 1, 2), servidor.paginasPedidas)
assertEquals(45, resultado.getOrThrow().size)
assertFalse(resultado.getOrThrow().single { it.id == 21L }.activo)
assertEquals(7L, resultado.getOrThrow().single { it.id == 21L }.categoriaId)
assertTrue((postIncierto as ResultadoEnvio.Fallo).error.efectoIncierto)
assertEquals(listOf("token-A"), servidor.tokensRecibidos)
assertEquals("DATOS_INCONSISTENTES", falloPagina.codigo)
```

Preparar páginas 20/20/5; para la credencial, pausar el motor después de capturar A y cambiar la sesión falsa a B antes de continuar. La solicitud A se cancela o usa A; nunca B.
- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest --tests "pe.edu.upeu.pharmamobile2.ProductoRemotoSesion13Test"`; comprobar fallos por comportamiento existente de una sola página y falta de clasificación.
- [ ] Implementar una pasada completa: pedir orden `id asc`, validar número de página, `totalPaginas`, `ultima` y progreso; rechazar IDs repetidos y metadatos incompatibles. Buffer provisional; no escribir SQL en esta clase. No implementar backoff aquí: T08 repite la pasada completa.
- [ ] Centralizar autorización de peticiones privadas con el token capturado para la cuenta/generación. Retirar el fallback del bearer global que podría cargar el token de otra cuenta o reenviar automáticamente una petición privada tras 401. `/me` usa explícitamente el token que intenta recuperar; login no lo recibe. Mantener sanitización de Authorization en logs.
- [ ] Traducir en datos: GET timeout/red/servidor -> TRANSITORIO; 400/404/409 -> PERMANENTE; 401 -> SESION; 403 -> PERMISOS. POST timeout/servidor/JSON inválido tras envío -> `efectoIncierto=true`. Propagar `CancellationException` y contexto obsoleto, no envolverlos en `Result.failure` normal. Mensajes externos no sanitizados no van a telemetría.
- [ ] Actualizar o retirar el repositorio REST anterior para que no quede como implementación activa paralela. Ejecutar pruebas remotas antiguas y nuevas. **Cierre:** el remoto no pierde páginas, no fabrica estado activo ni toma el token de otra cuenta.

### Task 6: T06 — Devolver guardado local inmediato y solicitar sincronización sin esperar HTTP

**Depende de:** T05. **Modificar:** `C/data/repository/ProductoRepositoryOffline.kt`, `C/domain/usecase/RegistrarProductoUseCase.kt`, Koin y `ProductoRepositoryOfflineTest`. **Crear:** `C/data/sync/DisparadorSincronizacion.kt`, `C/domain/model/MotivoSync.kt`, `T/GuardadoLocalInmediatoTest.kt`.

**Consume:** cola local y contexto. **Produce:** `registrar(producto): Producto` termina tras la transacción local; `DisparadorSincronizacion.solicitar(motivo: MotivoSync)` sin suspensión de red, con flujo de peticiones por cuenta; `MotivoSync = APERTURA | GUARDADO | CONECTIVIDAD | MANUAL | REVISION`.

- [ ] Escribir `guardarDevuelveAntesDeResolverRed`, `creacionesConcurrentesNoRepitenId` y `sinPermisoEscrituraNoCreaPendiente` con un remoto suspendido por `CompletableDeferred`:

```kotlin
assertTrue(guardado.isCompleted)
assertTrue(guardado.await().id < 0L)
assertEquals(1, cola.observar().first().size)
assertEquals(0, servidor.postsIniciados) // el test aún no arrancó el coordinador
assertEquals(ids.size, ids.toSet().size)
assertTrue(registroDePerfilSinCatalogoEscribir.isFailure)
assertEquals(0, pendientesDelPerfilSinPermiso)
```

- [ ] Ejecutar la clase específica y observar que la implementación anterior esperaba a sincronizar.
- [ ] Separar guardar de enviar; persistir producto con sus datos reales, ID local, operación y PENDIENTE. Exigir `CATALOGO_ESCRIBIR` en el perfil de esa cuenta para crear/actualizar/eliminar; en modo local se usa el último perfil validado sin afirmar que se conocen revocaciones remotas. Validar localmente nombre, precio y stock conservando los errores del formulario. Cambiar `runCatching` suspendido para volver a lanzar cancelación.
- [ ] Implementar disparador de cuenta con canal conflado: eventos próximos se agrupan; guardado sigue persistido aunque se cierre la app antes de consumir el evento. El coordinador T08 también examina pendientes en apertura; el canal no es el almacén durable de datos.
- [ ] Actualizar la expectativa del mensaje de UI a éxito local; retirar la llamada a `sincronizar()` dentro de `registrar()`. Mantener actualización/eliminación en línea, sin añadirlas a cola ni reintentarlas automáticamente.
- [ ] Ejecutar nuevas y antiguas pruebas de registro. **Cierre:** el formulario no espera un timeout para confirmar que el dato quedó en este equipo.

### Task 7: T07 — Enviar por fila con exclusión y conservar resultados inciertos

**Depende de:** T06. **Crear:** `C/domain/usecase/EnviarPendientesUseCase.kt`, `T/EnvioPendientesTest.kt`, `T/testutil/EscenarioSync.kt`. **Modificar:** adaptadores de cola y pruebas offline.

**Consume:** `ColaProductoRepository`, `ProductoRemotoRepository`, auth/contexto, reloj. **Produce:** `EnviarPendientesUseCase.invoke(): ResumenEnvios` donde `ResumenEnvios(enviadas: Int, detenerPorSesion: Boolean, detenerPorRed: Boolean)` permite a T08 decidir si debe continuar. Fixture `EscenarioSync` compone FixtureLocal + ServidorSimulado + auth falso + reloj/scheduler; expone `cola`, `local`, `ctxA`, `ctxB`, `auth`, `servidor`, `enviar`, y se amplía con coordinador/descarga en T08. Nunca usa Firebase.

- [ ] Escribir `validacionNoBloqueaOtraFila`, `creacionSinRespuestaNoSeReenvia`, `cincoIntentosAgotan`, `cancelaSinConsumirLasOtrasFilas` y `confirmacionNoPierdePendientes`:

```kotlin
assertEquals(EstadoEnvio.FALLIDA_PERMANENTE, cola.porId(rechazada)!!.estado)
assertEquals(1, resumen.enviadas)
assertEquals(1, servidor.creados) // POST aplicado, respuesta perdida
assertEquals(1, servidor.postsIniciados) // tras dos llamadas automáticas y reapertura
assertEquals(EstadoEnvio.REVISION, cola.porId(incierta)!!.estado)
assertEquals(0, cola.porId(noIniciada)!!.intentos)
```

- [ ] Ejecutar la clase nueva; esperar fallos específicos de detenerse en la primera fila o volver a enviar inciertas.
- [ ] Implementar selección por cuenta, compare-and-set `iniciar`, un único POST por intento autorizado y resolución en transacción. Un 400/404/409 afecta esa fila y permite continuar; 401 detiene cola y descarga; 403 bloquea la operación y hace que nuevas filas no se envíen hasta revisar la autorización. Una pérdida global de red no gasta intentos de filas no iniciadas.
- [ ] Si el POST comenzó y se cancela/interrumpe, dejar ENVIANDO si no puede actualizarse con seguridad; al recuperar la cuenta convertirlo en REVISION. No capturar la cancelación como éxito local remoto. Marcar resultado incierto por prioridad incluso si la excepción subyacente es transitoria.
- [ ] Confirmar cambiando el ID temporal por el remoto y conservando el resto de pendientes en una transacción. Contabilizar los cinco intentos iniciados; no contar selección, ausencia de red o lecturas.
- [ ] Ejecutar Envios y ColaProductoLocalTest. **Cierre:** un registro inválido no bloquea los válidos y un resultado incierto nunca genera un segundo POST automático.

### Task 8: T08 — Coordinar subida, descarga completa y backoff persistido

**Depende de:** T07. **Crear:** `C/domain/usecase/DescargarCatalogoUseCase.kt`, `C/data/sync/CoordinadorSincronizacion.kt`, `T/DescargaCatalogoTest.kt`, `T/CoordinadorSincronizacionTest.kt`. **Modificar:** `SincronizarProductosUseCase.kt`, `DescargaLocal.kt`, Koin; ampliar EscenarioSync con `descargar`, `coordinador` y `reiniciarCoordinador()` usando la misma DB.

**Consume:** envío T07, pasada remota T05, estado de descarga local, política T01 y disparador T06. **Produce:** `DescargarCatalogoUseCase.invoke(reiniciarAgotada: Boolean = false): Boolean`; `CoordinadorSincronizacion.sincronizar(motivo: MotivoSync): ResumenSincronizacion`, `actividad: StateFlow<Boolean>`, `suspend detener()`; `SincronizarProductosUseCase.invoke(): ResumenSincronizacion` delega a este coordinador, no al CRUD.

- [ ] Escribir `subeAntesDeListar`, `paginaIntermediaFallidaConservaCatalogoYFecha`, `esperasSonUnoDosCuatroYOcho`, `agotadaSoloReiniciaConAccionYPlazo`, `reinicioNoRenuevaPresupuesto`, `retrocesoRelojNoProduceEsperaInfinita` y `eventosConcurrentesNoDuplicanCiclo`:

```kotlin
assertEquals(listOf(1000L, 2000L, 4000L, 8000L), espera.demoras)
assertEquals(5, recorridosIniciados)
assertEquals(EstadoCadena.AGOTADA, descarga.actual()!!.estado)
assertEquals(timestampAnterior, descarga.observarUltimaSync().first())
assertEquals(catalogoAnterior, local.listar())
assertFalse(descargar(reiniciarAgotada = true)) // agotamiento + 15_999 ms
assertTrue(descargar(reiniciarAgotada = true))  // agotamiento + 16_000 ms, remoto sano
```

- [ ] Ejecutar las dos clases con `runTest`; comprobar fallos. Las demoras se verifican con tiempo virtual, nunca esperando 15 segundos reales.
- [ ] Implementar una cadena persistida por cuenta: marcar el recorrido iniciado antes del primer GET, reunir todas las páginas y conciliar una sola vez si son válidas. Cada fallo transitorio consume recorrido; nuevo recorrido desde página 0. Un fallo permanente bloquea y un 401 pide sesión, sin cuatro reintentos adicionales.
- [ ] Persistir estado/contador/instante y usar `Espera`. Un reinicio conserva contador. Si el reloj retrocede, no ampliar validez de token y acotar cualquier espera reanudada a 16 s, manteniendo el contador; conservar plazo agotado relativo de forma conservadora. Si los metadatos son inválidos, bloquear la descarga con mensaje de recuperación, no descartar catálogo.
- [ ] Implementar exclusión mutua y consumo conflado: eventos durante un ciclo generan como máximo una pasada posterior, no una por pulsación. Antes de cada efecto comprobar contexto; no mantener transacciones durante red o delay. No crear scopes globales sin cancelación.
- [ ] Retirar sincronización del contrato CRUD y su implementación antigua, migrando todos sus consumidores y fakes al coordinador en esta tarea. La lista sigue observando local. Solo `conciliarCompleta` cambia timestamp; revisión/fallidas no se borran por la descarga.
- [ ] Ejecutar ambas clases y regresión offline. **Cierre:** presupuesto y orden verificables; ciclo completo no equivale a cola totalmente resuelta y el resumen lo distingue.

### Task 9: T09 — Acciones individuales de revisión y recuperación

**Depende de:** T08. **Crear:** `C/domain/usecase/RevisarEnvioUseCase.kt`, `T/RevisionEnviosTest.kt`. **Modificar:** cola y persistencia de revisiones si es necesario.

**Consume:** cola, auth/contexto y disparador. **Produce:** `DecisionEnvio = MANTENER | REENVIAR | DESCARTAR | REHABILITAR`; `RevisarEnvioUseCase.invoke(id: Long, decision: DecisionEnvio, aceptaRiesgo: Boolean = false): Result<Unit>` y `corregir(id: Long, producto: Producto): Result<Unit>`.

- [ ] Escribir `reenviarSinAceptarRiesgoNoHacePost`, `descartarSoloBorraLocal`, `corregirInciertaNoCambiaPayload`, `corregirRechazoConservaDatosYHabilitaRevision` y `rehabilitarNoOcurrePorSincronizar`:

```kotlin
assertTrue(revisar(id, DecisionEnvio.REENVIAR, false).isFailure)
assertEquals(1, servidor.postsIniciados)
assertEquals(productoOriginal, cola.porId(id)!!.producto)
assertEquals(1, despuesDeCorregir.revision)
assertEquals(0, despuesDeCorregir.intentos)
assertEquals(0, servidor.deletesRecibidos)
```

Añadir `deletesRecibidos` al fixture remoto para comprobar que descartar no llama DELETE.
- [ ] Ejecutar clase específica y observar fallos de autorización/transición.
- [ ] Implementar validación de cuenta, estado y sesión: reenvío/rehabilitación requieren sesión remota válida y confirmación; mantener y descartar local se permiten al propietario en modo local. La UI confirma descarte antes de llamar al caso de uso.
- [ ] Reenviar no significa «ya confirmado»: liberar una vez para el coordinador con payload inmutable y conservar trazabilidad de revisión. Un resultado por nombre/precio no se vincula automáticamente al registro remoto.
- [ ] Mantener contador y espera por operación; al agotar cinco, exigir acción distinta de rehabilitación. En la nueva revisión guardar motivo/revisión anterior sin copiar mensajes crudos.
- [ ] Ejecutar clase y escenarios POST incierto. **Cierre:** cada acción tiene efecto local/remoto explícito y no hay «reintentar todo» que libere inciertas.

### Task 10: T10 — Cerrar la carrera de cambio de cuenta y aislar demostraciones

**Depende de:** T09. **Crear:** `C/domain/port/GuardiaCuenta.kt`, `T/AislamientoCuentaTest.kt`. **Modificar:** ScopesCuenta, auth, coordinador, commits de DB, `InMemoryRepository.kt`, App y detalle de producto.

**Consume:** contexto/generación y jobs del scope. **Produce:** `GuardiaCuenta.esActual(contexto: ContextoCuenta): Boolean`, `suspend fun <T> escribirSiVigente(contexto: ContextoCuenta, bloque: () -> T): T`; misma exclusión para invalidar una cuenta y autorizar escritura breve. `InMemoryRepository.limpiarDatosDeSesion()` restablece solo sus datos de demostración, no el catálogo SQL.

- [ ] Escribir `respuestaTardiaDeANoEscribeEnB`, `salidaConPostEnCursoNoReenviaAlVolver`, `borradorAInvisibleEnB` y `demoNoConservaClienteDeOtroUsuario`:

```kotlin
assertEquals(emptyList(), localB.listar())
assertNull(borradorB.observar().first())
assertFalse(auth.esActual(ctxAnteriorA))
assertEquals(0, avisosDeOtraCuenta)
assertFalse(clientesDemoTrasCambio.any { it.nombre == "Cliente privado A" })
```

Para la respuesta tardía, un fake devuelve después de cancelarse el job; comprobar guardia antes de cualquier escritura/aviso, no solo antes de lanzar red.
- [ ] Ejecutar clase y comprobar fallo de generación/limpieza previo al endurecimiento.
- [ ] Al cerrar: invalidar contexto, cancelar y esperar jobs, quitar observadores, limpiar token/cache de auth, borrador visible y avisos; mantener filas/borradores privados. La guardia protege el tramo SQL sin sostener el lock durante I/O. Si no llega respuesta válida, la fila A permanece interrumpida hasta su propia recuperación.
- [ ] Comprobar que GET de detalle y compartir solo resuelven IDs desde el scope activo; cancelar un detalle anterior al cambiar de cuenta. No reutilizar un ViewModel con productoOriginal de otra cuenta.
- [ ] Vaciar estados de clientes/pedidos en salida/cambio. Etiquetar esos módulos como demostración; sacar el botón técnico «Test 403/200» y acceso Ktor directo del flujo normal, manteniéndolo solo en herramientas de desarrollo si se conserva. No añadir REST de clientes/pedidos aquí.
- [ ] Ejecutar aislamiento, auth, detalle y offline. **Cierre:** ninguna respuesta de generación caducada afecta una cuenta nueva. Cerrar paso 3 con su commit.

### Task 11: T11 — Exponer resumen, borradores y acciones en Inicio y Productos

**Depende de:** T10. **Crear:** `C/presentation/sincronizacion/SincronizacionUiState.kt`, `SincronizacionViewModel.kt`, `ResumenSincronizacionCard.kt`, `RevisionEnviosScreen.kt`; `T/SincronizacionViewModelTest.kt`, `T/BorradorProductoViewModelTest.kt`. **Modificar:** ProductoUi/UiState/ViewModel/Screen, InicioScreen, DetalleProductoViewModel, SesionViewModel, `Screen.kt`, App y Koin; pruebas de ViewModel existentes.

**Consume:** catálogo/cola/fecha observables, coordinador, borrador y revisión. **Produce:**

```kotlin
data class SincronizacionUiState(
    val enCurso: Boolean = false,
    val pendientes: Int = 0,
    val fallidas: Int = 0,
    val revision: Int = 0,
    val bloqueadas: Int = 0,
    val ultimaSync: Long? = null,
    val antiguedad: String = "Nunca sincronizado",
    val mensaje: String? = null,
    val puedeReintentarDescarga: Boolean = false
)
```

`SincronizacionViewModel.estado: StateFlow<SincronizacionUiState>`, `sincronizar()`, `reintentarDescarga()`, `revisar(id, decision, aceptaRiesgo)`. `Screen.RevisionEnvios`; acción visible «Revisar envíos». `ProductoUi` añade estado de envío/motivo para sus badges, sin importar tipos SQL.

- [ ] Escribir `contadoresNoOcultanFallidas`, `antiguedadCambiaSinNuevoTimestamp`, `formularioRestauraSoloSuCuenta` y `guardarNoDiceConfirmadoSiSoloEsLocal`:

```kotlin
assertEquals(0, estado.pendientes)
assertEquals(1, estado.fallidas)
assertEquals(1, estado.revision)
assertEquals("Actualizado hace 2 h", trasAvanzarReloj.antiguedad)
assertEquals("Guardado en este equipo. Pendiente de envío", productoVm.uiState.value.mensajeExito)
assertEquals("12.", formularioRestaurado.precio)
```

- [ ] Ejecutar ambas clases y ProductoViewModelTest; comprobar fallos por contador incompleto o pérdida de borrador.
- [ ] Implementar `combine` de flujos locales y actividad; reloj observable cada 60 s mientras la UI esté suscrita, sin HTTP para pintar estado. Conservar lista y errores por campo durante operaciones. Conectividad del dispositivo y error del servidor se exponen como conceptos distintos.
- [ ] Persistir cambios de borrador de forma serializada por cuenta; guardar antes de cambiar pantalla o reautenticar. No restaurar un borrador antiguo tras un guardado: borrar/consumir borrador junto al alta local en la misma transacción o mediante una revisión/versionado que impida la carrera con escrituras tardías.
- [ ] Añadir resumen a Inicio y Productos, usando mismo ViewModel del scope. Mostrar PENDIENTE, FALLIDA, REVISION y bloqueo con acciones diferentes. En «Reenviar», texto obligatorio: «El servidor podría haber registrado este producto. Reenviarlo puede duplicarlo». Pedir confirmación específica para descarte/rehabilitación y no esconderla en el botón general.
- [ ] Expiración muestra login sin perder borrador; volver a la misma cuenta permite continuarlo y otra cuenta no lo ve. Añadir estados vacío/cargando/error recuperable. Eliminar botón de «simular antigüedad» del flujo normal y su método del contrato CRUD; si se conserva para docencia, solo desarrollo.
- [ ] Ejecutar pruebas ViewModel y recorrer la UI con tamaños de teléfono/tablet y ambos temas. **Cierre:** contadores y acciones coherentes, sin éxito remoto falso. Cerrar paso 4 con su commit.

### Task 12: T12 — Gestor de permisos y puente Android ligado a Activity

**Depende de:** T11. **Crear:** `C/domain/port/GestorPermisos.kt`, `A/data/permiso/GestorPermisosAndroid.kt`, `PuenteSolicitudPermiso.kt`, `APP/permiso/PuentePermisosActivity.kt`, `H/GestorPermisosAndroidTest.kt`, `AT/PuentePermisosActivityTest.kt`. **Modificar:** MainActivity, manifiesto, Android Koin y Gradle app para instrumentación.

**Consume:** acción voluntaria de activar avisos. **Produce:**

```kotlin
enum class Permiso { NOTIFICACIONES }
sealed interface EstadoPermiso {
    data object Concedido : EstadoPermiso
    data object Denegado : EstadoPermiso
    data object DenegadoParaSiempre : EstadoPermiso
}
interface GestorPermisos {
    suspend fun estado(permiso: Permiso): EstadoPermiso
    suspend fun solicitar(permiso: Permiso): EstadoPermiso
    fun abrirAjustes()
}
```

Puente Android en shared declara `suspend solicitar(nombre: String): ResultadoSolicitudAndroid`; resultado `Concedido | Rechazado | Cancelado`. La implementación en androidApp registra Activity Result en `onCreate` y se vincula/desvincula al adaptador compartido. No hacer que shared dependa del módulo androidApp.

- [ ] Escribir tabla de estado como pruebas: primer uso sin historial -> Denegado; denegación solicitable con rationale -> Denegado; denegación confirmada sin rationale -> DenegadoParaSiempre; concesión -> Concedido. Añadir `cancelarDialogoNoMarcaDenegacionDefinitiva`, `dosSolicitudesNoPierdenContinuacion`, `activityDestruidaNoSeRetiene` y `api32NoSolicitaPostNotifications`:

```kotlin
assertIs<EstadoPermiso.Denegado>(primeraConsulta)
assertIs<EstadoPermiso.Denegado>(despuesDeCancelar)
assertIs<EstadoPermiso.DenegadoParaSiempre>(rechazoDefinitivo)
assertEquals(1, lanzamientosConDosSolicitudes)
assertEquals(0, lanzamientosEnApi32)
```

Pruebas host usan fachada nativa falsa; instrumentación comprueba registro/lifecycle reales. El test RBAC `PermisosTest.kt` existente no se reutiliza como evidencia de permisos del SO.
- [ ] Ejecutar host nuevo; comprobar fallos. En app configurar `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` y dependencias de prueba Android usando catálogo existente/artefactos compatibles, sin actualizar versiones de UI como efecto secundario.
- [ ] Implementar consulta runtime API 33+, bandera de solicitud y resultado. Registrar lanzamiento no equivale a registrar una respuesta de denegación. Conservar cancelación cuando Activity Result no trae resultado; no reducir toda cancelación a Boolean false. Serializar solicitudes y limpiar continuaciones canceladas/desvincular Activity.
- [ ] Añadir `<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>`. En API 24-32 no pedir runtime permission inexistente; bloqueo de avisos por app se resuelve mediante Ajustes. Al volver de Ajustes consultar estado real.
- [ ] Añadir explicación «Activa los avisos para saber si tus cambios se enviaron o necesitan revisión», botones «Activar» y «Ahora no», y acción Ajustes cuando corresponda. No bloquear catálogo ni iniciar la solicitud automáticamente al lanzar la app.
- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest --tests "pe.edu.upeu.pharmamobile2.GestorPermisosAndroidTest"`; después `.\gradlew.bat :androidApp:connectedDebugAndroidTest` con AVD de prueba. Validar deslizar/cancelar en el SO real: si una versión entrega resultado ambiguo, mantener estado solicitable conservador y dejar Ajustes accesible, sin inventar una denegación definitiva.
- [ ] **Cierre:** tres estados, cancelación y retorno desde Ajustes comprobados; falta conectar aviso real en T13.

### Task 13: T13 — Avisos locales, conectividad en primer plano y tap seguro

**Depende de:** T12. **Crear:** `C/domain/port/NotificadorSincronizacion.kt`, `MonitorConectividad.kt`, `C/data/notificacion/PoliticaAvisos.kt`, `A/data/notificacion/NotificadorAndroid.kt`, `A/data/conectividad/MonitorConectividadAndroid.kt`, `T/PoliticaAvisosTest.kt`, `AT/NotificacionesSincronizacionTest.kt`; icono monocromo en `androidApp/src/main/res/drawable/ic_sync_notification.xml`. **Modificar:** coordinador, MainActivity, plataforma Koin y preferencias de cuenta.

**Consume:** resultado del coordinador y permiso. **Produce:** en `NotificadorSincronizacion`, `suspend fun publicar(contexto: ContextoCuenta, resumen: ResumenSincronizacion): Unit` y `suspend fun cancelar(contexto: ContextoCuenta): Unit`; `MonitorConectividad.estado: StateFlow<EstadoConectividad>` con `DESCONOCIDA | DISPONIBLE | NO_DISPONIBLE`; `PoliticaAvisos.debeNotificar(anterior: ResumenSincronizacion?, actual: ResumenSincronizacion, habilitada: Boolean): Boolean`; preferencia por cuenta en metadatos. Mientras la señal sea DESCONOCIDA no afirmar «Sin conexión».

- [ ] Escribir `sinCambiosNoNotifica`, `mismoFalloNoSeRepite`, `denegarNoImpideSincronizar`, `salidaCancelaAviso`, `tapConOtraCuentaNoMuestraDatos`:

```kotlin
assertFalse(politica.debeNotificar(anterior, anterior, true))
assertEquals(1, avisosPublicadosTrasDosResumenesIguales)
assertTrue(resultadoSinPermiso.descargaCompleta)
assertEquals(0, avisosActivosTrasSalir)
assertEquals(DestinoAviso.LOGIN, resolverDestino(cuentaAvisoA, cuentaActivaB))
```

`DestinoAviso = LOGIN | REVISION_ACTUAL`; `resolverDestino(cuentaAviso, contextoActivo)` es una función pura de PoliticaAvisos, no activa ninguna cuenta.
- [ ] Ejecutar pruebas puras y observar fallos de duplicación/destino antes de implementar.
- [ ] Crear canal `sincronizacion`, importancia normal y texto solo de cantidades. Comprobar permiso, bloqueo general y bloqueo de canal antes de publicar. Capturar fallos de notificación sin cambiar el resultado de negocio. ID/tag por cuenta no contiene username: usar identificador interno/hash local.
- [ ] Conectar resumen terminado con preferencia voluntaria: avisar por confirmaciones o nuevas fallidas/revisión, no por toda descarga vacía. Persistir huella por cuenta del último aviso para no repetirlo al rotar/reabrir. Usar PendingIntent explícito e inmutable; al tocar validar cuenta vigente, nunca confiar en la cuenta del extra para autenticar.
- [ ] Conectar callback de conectividad mientras la app está activa, con registro/desregistro simétrico. Reagrupación usa el disparador existente; red recuperada no libera filas en revisión. Registrar ACCESS_NETWORK_STATE si la API usada lo requiere, sin diálogo runtime adicional.
- [ ] Ejecutar políticas + instrumentación Android: recepción real, app/canal bloqueado, denegación, tap y salida. **Cierre:** permiso tiene uso real y el aviso no filtra datos ni duplica eventos. No prometer recepción de sincronizaciones que nunca se ejecutan con la app cerrada.

### Task 14: T14 — Preparar permisos y avisos iOS sin declarar validación inexistente

**Depende de:** T13. **Crear:** adaptadores I de permisos, notificaciones y conectividad; `docs/sesion13/06-verificacion-ios-pendiente.md`. **Modificar:** `I/di/PlatformModule.ios.kt`, punto de entrada iOS y configuración que realmente requiera UserNotifications.

**Consume:** mismos contratos comunes. **Produce:** implementaciones iOS registradas y guion de compilación/diálogos pendiente de Mac. UserNotifications se inicializa para la app y prepara su comportamiento en primer plano; conectividad usa el monitor nativo disponible con cancelación.

- [ ] Añadir casos de fake común al flujo de permisos: `notDetermined -> Denegado`, `denied -> DenegadoParaSiempre`, autorizado -> Concedido. No afirmar que esos fakes prueban el adaptador iOS.
- [ ] Implementar solicitud y consulta con UserNotifications; completions se traducen a coroutines cancelables. Abrir Ajustes mediante la plataforma; no importar UIKit en commonMain.
- [ ] Implementar avisos con el mismo texto seguro y política de cuenta, incluida cancelación al salir y resolución de tap. Registrar las implementaciones en Koin iOS.
- [ ] No añadir NSCameraUsageDescription, ubicación ni APNs entitlements: el alcance usa avisos locales. Revisar Info.plist por lo aplicable, sin inventar una clave de «descripción de notificaciones».
- [ ] Documentar comandos por ejecutar EN MAC: `./gradlew :shared:iosSimulatorArm64Test`; `xcodebuild -list -project iosApp/iosApp.xcodeproj`; después build del esquema listado, destino iOS Simulator, signing desactivado para simulador. Elegir un simulador instalado mediante `xcrun simctl list devices available`, no un modelo inventado.
- [ ] Registrar cada resultado como `PREPARADO / NO EJECUTADO EN MAC`. **Cierre:** revisión estática y contratos coherentes; no declarar compilación ni notificación iOS obtenida. Cerrar paso 5 con su commit y evidencias Android disponibles.

### Task 15: T15 — Crashlytics Android real con interfaz y sanitización

**Depende de:** T14. **Crear:** `C/domain/port/Telemetria.kt`, `C/data/firebase/TelemetriaFirebase.kt`, `TelemetriaSegura.kt`, `T/TelemetriaSeguraTest.kt`. **Modificar:** Gradle raíz/shared/androidApp, `gradle/libs.versions.toml`, Application, Koin y `T/testutil/DoblesSesion13.kt` para añadir TelemetriaFalsa; configuración externa `androidApp/google-services.json` provista para el proyecto real. Documentar configuración en `02-configuracion.md`.

**Consume:** `Telemetria` sin SDK y eventos del coordinador. **Produce:**

```kotlin
interface Telemetria {
    fun registrarError(e: Throwable, contexto: Map<String, String> = emptyMap())
    fun registrarEvento(nombre: String, datos: Map<String, String> = emptyMap())
}
```

`TelemetriaSegura(delegate: Telemetria)` acepta solo claves `pantalla`, `operacion`, `tipo_error`, `estado`, `cantidad`; valores de pantalla/operación/tipo/estado pertenecen a enumeraciones controladas, cantidad se valida como entero no negativo. No basta con permitir cualquier texto bajo una clave permitida. `TelemetriaFirebase` registra excepción sanitizada y breadcrumbs, sin añadir Analytics.

- [ ] Escribir `mensajeYCausaNoExponenToken`, `contextoPermitidoNoAceptaDatosLibres`, `proveedorRotoNoRompeGuardado` y `cancelacionNoSeReportaComoCrash`:

```kotlin
assertFalse(salidaProveedor.contains("Bearer secreto"))
assertFalse(salidaProveedor.contains("persona@correo.test"))
assertFalse(salidaProveedor.contains("Producto privado"))
assertEquals(0, erroresRegistradosPorCancelacion)
assertTrue(guardadoConProveedorQueFalla.isSuccess)
```

Preparar excepción con esos datos en mensaje, causa y suppressed exceptions. La versión que llega al proveedor usa código y mensaje técnico fijo sin encadenar la excepción original; conservar stack técnico solo si no incorpora esos textos.
- [ ] Ejecutar la clase; observar fallos de sanitización antes del adaptador real. El proveedor falso registra exactamente lo recibido, no una versión ya filtrada por el test.
- [ ] Comprobar proyecto Firebase del usuario, registrar el applicationId actual y obtener JSON válido. Si faltan acceso/archivo, seguir con tests y adaptador pero marcar integración real bloqueada por ese recurso; no fabricar JSON ni crear una cuenta. El build integrado debe fallar de forma clara por configuración ausente, no pasar con un no-op oculto.
- [ ] Fijar candidatos: GitLive `dev.gitlive:firebase-crashlytics:2.7.0` conforme a guía, Google Services `4.5.0` y plugin Crashlytics `3.0.8` según documentación oficial consultada. Declarar alias raíz `apply false` y aplicarlos en androidApp; añadir SDK en shared. Ejecutar resolución/build; si hay incompatibilidad, aislarla y documentar versión compatible antes de cambiar el pin, sin actualizar Kotlin/Compose/AGP en bloque. Nunca usar versiones dinámicas `+`.
- [ ] Implementar adaptadores con las APIs de la versión fijada: excepción no fatal y log/breadcrumb. Inyectar la envoltura segura en casos de uso/coordinador; errores normales de falta de red y cancelación no generan spam. Ningún import GitLive en dominio o presentación.
- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest --tests "pe.edu.upeu.pharmamobile2.TelemetriaSeguraTest"` y `.\gradlew.bat :androidApp:assembleDebug`. Emitir un fallo controlado no fatal desde una herramienta de desarrollo, permitir su envío y comprobar recepción en Firebase Console; conservar evidencia E16 sin datos personales.
- [ ] **Cierre:** compilación y captura de recepción real; si falta el recurso externo, el criterio Firebase permanece pendiente aunque los tests falsos pasen. No incluir cuentas de servicio ni contraseñas en Git. La ubicación/versionado del JSON de cliente se documenta según política del repositorio; no confundirlo con una cuenta de servicio.

### Task 16: T16 — Preparar enlace Firebase iOS y configuración reproducible

**Depende de:** T15. **Modificar:** `iosApp/iosApp.xcodeproj/project.pbxproj`, `iosApp/iosApp/iOSApp.swift`, `iosApp/Configuration/Config.xcconfig`, Koin iOS y `06-verificacion-ios-pendiente.md`. **Recurso externo futuro:** `iosApp/iosApp/GoogleService-Info.plist` del bundle final.

**Consume:** adaptador común GitLive y proyecto iOS existente. **Produce:** integración prevista mediante Swift Package Manager de `https://github.com/firebase/firebase-ios-sdk` con FirebaseCore y FirebaseCrashlytics, inicialización nativa antes de resolver telemetría y lista de tareas por ejecutar en Mac.

- [ ] Registrar el bundle actual: `pe.edu.upeu.pharmamobile2.PharmaMobile2$(TEAM_ID)` según Config.xcconfig. Fijar/confirmar el bundle final para registrar Firebase; no descargar un plist de otro bundle ni inventar TEAM_ID/certificado. Mantener el identificador aprobado y señalar el recurso pendiente si aún no puede registrarse.
- [ ] Preparar las referencias SPM y productos nativos que GitLive no enlaza automáticamente. Tomar el rango compatible del SDK iOS documentado para GitLive 2.7.0 y fijar la resolución en Mac; no afirmar compatibilidad porque el adaptador común compila en Android.
- [ ] Inicializar FirebaseCore en el punto de entrada Swift antes de Koin/telemetría. Documentar ubicación del plist y su pertenencia al target; configurar scripts de símbolos/dSYM de Crashlytics únicamente con rutas verificadas en el entorno Mac.
- [ ] Añadir al guion Mac: resolver paquetes; ejecutar tests KMP iOS; compilar simulador; ejecutar fallo no fatal y confirmar recepción; probar permisos/avisos y cambio de cuenta. Cada fila lleva resultado pendiente, comando, evidencia prevista y recurso requerido.
- [ ] En Windows realizar revisión estática de imports, configuración y contratos, y repetir tests Android solo si se tocó commonMain. **Cierre:** iOS preparado, ninguna evidencia ni firma simulada. Cerrar paso 6 con su commit cuando la parte Android de T15 esté realmente acreditada; si está bloqueada, registrar ese estado sin fingir cierre.

### Task 17: T17 — Ejecutar regresión integrada y verificar límites de arquitectura

**Depende de:** T16, con pendientes externos registrados si existen. **Modificar:** suites existentes para los contratos finales, `T/IntegradorSesion13Test.kt`; herramientas de desarrollo solo si hacen falta para evidencia. **Consume:** toda la integración. **Produce:** logs y reportes reales, sin números heredados del README.

- [ ] Escribir el escenario integrado `crearOfflineReautenticarSincronizarYSalir`: entrar online A, guardar sin red, restaurar A en modo local, volver online, autenticar, enviar una vez, confirmar, observar contador/catálogo, cerrar y entrar B. Añadir `postInciertoSobreviveReinicioSinReenvio` y `migracionNoContaminaCuentaNueva`:

```kotlin
assertEquals(1, servidor.creados)
assertEquals(1, servidor.postsIniciados)
assertEquals(0, estadoA.pendientes)
assertEquals(1, estadoIncierto.revision)
assertTrue(productosB.isEmpty())
assertNull(borradorB.observar().first())
```

- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest --tests "pe.edu.upeu.pharmamobile2.IntegradorSesion13Test"`. Si falla, corregir la causa en su tarea propietaria y repetir solo las pruebas afectadas más este escenario.
- [ ] Ejecutar `.\gradlew.bat :shared:testAndroidHostTest`, tarea de verificación SQL identificada en T00, `.\gradlew.bat :androidApp:assembleDebug` y `.\gradlew.bat :androidApp:lintDebug`. Esperado: BUILD SUCCESSFUL y cero fallos de tests; comparar lint con línea base, corregir problemas nuevos pertinentes. No repetir todo sin cambios o indicios nuevos.
- [ ] Ejecutar `.\gradlew.bat :androidApp:connectedDebugAndroidTest` en el AVD de prueba y recoger reportes. Si no hay dispositivo conectado, reportar esa prueba como pendiente en lugar de interpretar la ausencia como éxito.
- [ ] Verificar imports con `rg`: en `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/domain` y `presentation` no debe haber `dev.gitlive`, `com.google.firebase`, `android.*`, `platform.UIKit` ni tipos Ktor. Los adaptadores de datos/plataforma y archivos DI sí pueden referenciar sus implementaciones. Buscar también accesos SQL sin cuenta, catches que absorban cancelación y llamadas HTTP directas desde pantallas.
- [ ] Revisar diff completo: no cambios backend, nuevas dependencias fuera de alcance, archivos de secretos ni controles de prueba en el flujo normal. Revisar accesibilidad básica: estados con texto además del color, acciones alcanzables y explicación de permiso cancelable.
- [ ] Registrar total real de tests y fallos leyendo los XML generados; enlazar el HTML de Gradle. **Cierre:** evidencia automatizada actual y límites claros; no copiar el antiguo dato de «47 tests» como si fuera la ejecución de hoy.

### Task 18: T18 — Recorrido final, evidencias, informe y entrega

**Depende de:** T17. **Crear/modificar:** `docs/sesion13/01-recorrido-y-huecos.md`, `03-matriz-evidencias.md`, `04-informe.md`, `05-backlog-sesion14.md`, `06-verificacion-ios-pendiente.md`, `evidencias/`, `logs/` y README.

**Consume:** resultados reales de cada tarea. **Produce:** expediente de sesión 13 con los 14 criterios y decisiones de alcance; rama con commits por paso; limitaciones iOS/Firebase/dispositivo sin disimular.

- [ ] Repetir los ocho recorridos T00 sobre el build final. Actualizar cada hueco con estado cerrado/pendiente/descartado, prueba y captura; conservar la descripción inicial para mostrar qué cambió.
- [ ] Capturar E01-E19 de la matriz siguiente en AVD/cuentas de prueba. Los diálogos de permiso, Ajustes y notificación provienen de Android real; red simulada se etiqueta como prueba automatizada. No fabricar capturas ni alterar las imágenes para simular resultados.
- [ ] Completar los 14 criterios con `CUMPLE`, `NO CUMPLE` o `PENDIENTE` y enlaces verificables. Un criterio compuesto con una parte pendiente conserva el detalle; no convertirlo en cumplido por tener solo código.
- [ ] Escribir informe: objetivo, estado inicial, arquitectura, migración, política de errores/reintentos, límite de POST sin idempotencia, permiso real, Firebase, pruebas, evidencias, pasos Android y preparación iOS. Distinguir trabajo de sesión 13 y criterios finales de unidad: dispositivos físicos, builds firmados y sustentación.
- [ ] Registrar backlog sesión 14 con tareas concretas y dependencias: clientes REST/validación; pedidos frente al contrato de ventas; catálogo real compartido; política de stock y errores; persistencia según alcance posterior; recuperación asistida de legado; pruebas/evidencias iOS en Mac; distribución/build firmado de cierre de unidad. No implementar estos módulos como tarea oculta de sesión 13.
- [ ] Comprobar enlaces locales, nombres de evidencias, presencia real de archivos y ausencia de PII. Guardar commit SHA/build/API/dispositivo y comando asociado a cada evidencia técnica.
- [ ] Cerrar paso 7 con commit convencional. Entregar enlaces a informe y matriz y resumir verificación ejecutada, pendientes y riesgos. **Cierre:** la entrega es reproducible por otro estudiante sin conocer este chat.

## 5. Esquema local y consultas que deben quedar cubiertas

Nombres de columna de referencia para evitar divergencias entre tareas. SQLDelight mapea INTEGER a Long; adaptar a Int/Boolean en la frontera de datos. No cambiar la representación Double del precio en esta sesión.

| Tabla | Clave | Campos y restricciones esenciales |
| --- | --- | --- |
| `producto` | `(cuentaClave TEXT, id INTEGER)` | nombre TEXT, precio REAL, stock INTEGER; activo INTEGER nullable solo para legado incompleto, categoriaId INTEGER nullable, categoriaNombre TEXT nullable; datosIncompletos INTEGER 0/1; pendiente INTEGER 0/1; estadoEnvio TEXT con valores de enum; intentos INTEGER >=0; revision INTEGER >=0; operacionLocalId TEXT nullable solo en confirmados/legado; ultimoErrorCodigo TEXT nullable; ultimoError TEXT nullable; erroresCampos TEXT nullable; proximoIntentoEnMs INTEGER nullable |
| `metadato` | `(cuentaClave TEXT, clave TEXT)` | valor TEXT; última descarga completa, preferencia y huella de avisos por cuenta |
| `borrador_producto` | `cuentaClave TEXT` | idEdicion nullable, nombre/precioTexto/stockTexto TEXT, categoriaId/categoriaNombre nullable, version INTEGER para rechazar escritura tardía |
| `descarga` | `cuentaClave TEXT` | cadenaId TEXT, estado TEXT, intentos INTEGER 0..5, proximoEnMs nullable, agotadaEnMs nullable, ultimoErrorCodigo/mensaje nullable |
| `historial_envio` | `(cuentaClave, operacionLocalId, revision, intento)` | estado, código/mensaje seguro y instanteMs; sin cuerpos de petición/respuesta |

Para filas nuevas, activo/categoría se conservan desde Producto; no producir registros elegibles con datos requeridos ausentes. Legado incompleto nunca entra en consultas normales. `legacy_unassigned` es un namespace reservado, imposible de generar para una cuenta real.

Consultas mínimas verificadas por tests: listarPorCuenta; obtenerPorCuentaId; listarElegibles; contarPorEstado; observarPorEstado; siguienteIdLocalPorCuenta; insertarPendiente; iniciarSiElegible; resolverFallo; confirmarEnTransaccion; recuperarInterrumpidos; borrarSoloConfirmadosDeCuenta; conciliarConfirmadosCompletos; leer/guardar cadena; leer/guardar/consumir borrador con versión; leer/guardar metadato por cuenta; append/update historial.

No basta con filtrar SELECT: UPDATE y DELETE también deben incluir cuenta. No usar la misma ID negativa como identidad global de operación; el UUID local diferencia intentos/revisiones.

## 6. Matriz de evidencia final

Archivos bajo `docs/sesion13/evidencias/`. Los IDs son estables; se completa la extensión y ruta real en el informe. Una evidencia puede tener PNG y log relacionados. Todos los resultados empiezan pendientes.

| ID | Escenario y preparación | Prueba objetiva de aceptación | Origen |
| --- | --- | --- | --- |
| E01 | Primera apertura de AVD nuevo sin perfil, sin red | No entra como administrador; login y mensaje comprensibles | Captura Android |
| E02 | Backend accesible, credenciales incorrectas; luego sin red | Mensajes distintos para credencial y conectividad | Dos capturas Android |
| E03 | Cuenta conocida con caché vacía | Estado vacío propio y acciones disponibles | Captura Android |
| E04 | Cuenta previamente validada; modo avión; crear producto | Guardado local inmediato, pendiente visible y persiste al reiniciar | Capturas Android + prueba SQL |
| E05 | Simular POST aplicado con respuesta perdida | Un creado, un POST, fila en revisión tras reinicio y disparadores | Log de prueba MockEngine, rotulado simulado |
| E06 | Recuperar red con fila no intentada y sesión válida | Un envío, ID remoto y UI observable; backend confirma lectura del producto | Android + lectura REST sin editar backend |
| E07 | Respuesta 400/409 controlada y otra fila válida | Fila rechazada visible, campos/motivo y segunda fila enviada | Tests + captura del estado UI de prueba |
| E08 | Token vencido durante formulario | Borrador conservado; login mismo usuario lo recupera | Android + prueba sesión |
| E09 | A con pendientes, salir, entrar B | B no ve ni envía A; sin token A activo | Tests aislamiento + capturas sin datos personales |
| E10 | Catálogo de 45 productos; fallo página 1 | Todas las páginas o caché intacta, timestamp sin cambio ante fallo | Log de prueba paginación |
| E11 | Cinco fallos transitorios GET | Demoras 1/2/4/8, agotamiento y recuperación explícita tras 16 s | Log con tiempo virtual + captura de UI |
| E12 | Avisos primer uso/denegación/cancelación | Denegado solicitable; deslizar no lo vuelve definitivo | Captura real Android API 33+ |
| E13 | Denegación definitiva y Ajustes | Acción Ajustes; al conceder/volver cambia el estado | Capturas reales Android API 33+ |
| E14 | Permiso concedido y ciclo con cambios | Notificación local real con cantidades; tap seguro | Captura bandeja + app |
| E15 | Avisos denegados/app-canal bloqueados y repetición del mismo fallo | Sincronización sigue; no spam; diagnóstico apropiado | Test + comprobación Android |
| E16 | Error no fatal técnico en build integrado | Informe recibido en Crashlytics sin token/PII | Firebase Console + build/fecha |
| E17 | Migración v1 a v2 | Valores previos conservados en legado, esquema equivalente, sin cuenta inventada | Reporte migración |
| E18 | Suites host, instrumentación, assemble y lint | Logs/reportes actuales; número real de tests y fallos | Archivos Gradle |
| E19 | Preparación iOS | Archivos y guion presentes; verificación en Mac indicada pendiente | Documento, NO captura de ejecución iOS |

Para permiso concedido/denegado/definitivo usar un AVD propio API 33+ con target actual. Preferir restablecer permisos del AVD de pruebas con comandos oficiales y volver a abrir la app; no borrar datos reales para lograr el estado deseado. Registrar secuencia exacta y versión Android porque el sistema puede variar el comportamiento del diálogo.

Si se usa dispositivo físico, repetir E04/E06/E12-E15 y registrar modelo/API sin publicar identificadores del dispositivo. Si no existe, el criterio de prueba física del producto final queda declarado pendiente.

## 7. Matriz de los 14 criterios de la guía

| N.º | Criterio | Tareas propietarias | Pruebas/evidencias | Regla para marcar cumplido |
| --- | --- | --- | --- | --- |
| 1 | Recorrido y huecos | T00, T18 | E01-E09; tabla inicial/final | Ocho recorridos registrados y decisiones justificadas |
| 2 | Intentos y último error en fila | T03-T04 | E17; ColaProductoLocalTest | Migración preserva datos y campos cambian al intentar |
| 3 | Diferencia transitorio/permanente | T01, T05, T07 | E05/E07; tests remotos/envío | Clasificación visible y no se repite un rechazo permanente |
| 4 | Espera creciente y máximo | T01, T08 | E11 | Cinco recorridos GET y demoras verificadas; excepción POST manual explicada |
| 5 | Agotadas visibles | T04, T09, T11 | E11; RevisionEnviosTest | No desaparecen; motivo y rehabilitación explícita |
| 6 | Subida antes de bajada, sin pérdida | T07-T08 | E04-E06/E10/E17 | Confirmación/conciliación atómicas, pendientes preservados; límite no-idempotente declarado |
| 7 | Pendientes/fallidas/antigüedad | T11 | E03/E04/E07/E11 | Consultas observables y reloj; visibles en Inicio y Productos |
| 8 | Interfaz común de permisos | T12 | Tests + inspección de imports | Contrato sin Activity ni SDK nativo |
| 9 | Tres estados Android | T12 | E12-E13 | Estados comprobados contra el SO y tabla de historial |
| 10 | Manifest y configuración aplicable | T12, T14 | Manifest + E12-E14/E19 | POST_NOTIFICATIONS real; iOS aplicable preparado y claramente no verificado |
| 11 | Explicación y Ajustes | T12-T13 | E12-E15 | Cancelable y sin bloquear catálogo; retorno consulta estado real |
| 12 | Firebase en datos | T15-T16 | E16 + rg arquitectura | SDK encapsulado y recepción real Android |
| 13 | commonTest pasan | T01-T17 | E18 | Resultado actual del target host sin fallos, no reporte histórico |
| 14 | Commits distribuidos y convencionales | Todos, T18 | Historial de rama | Al menos un commit por cada paso, contenido acorde al mensaje |

La matriz no altera la rúbrica ni promete una nota. Explica cómo acreditar las decisiones compatibles con la advertencia de idempotencia de la guía y deja visibles los límites elegidos por el usuario.

## 8. Riesgos, recursos externos y criterio de parada

| Riesgo o recurso | Acción concreta | Qué puede seguir avanzando | Qué no se declara completo |
| --- | --- | --- | --- |
| No hay proyecto/configuración Firebase | Obtener proyecto y JSON correctos con el usuario en T15 | Contrato, sanitización y pruebas falsas explícitas | Recepción real e integración Firebase final |
| Gradle/SDK no resuelven una versión fijada | Guardar error, aislar compatibilidad y registrar cambio mínimo | Componentes que no dependan de ella | Build afectado |
| AVD/API 33+ no disponible | Preparar AVD de prueba con SDK, sin borrar dispositivo real | Tests host y código | Tres estados reales del permiso |
| Backend no disponible | Usar MockEngine para tests y registrar limitación | Arquitectura, SQL, UI y pruebas deterministas | Recorrido REST real |
| No se dispone de Mac | Ejecutar guion cuando haya acceso | Android completo y preparación estática iOS | Build/pruebas/evidencias iOS |
| Legacy sin propietario | Conservar en namespace reservado y advertencia sin detalles | Cuentas nuevas aisladas | Recuperación asistida del legado |
| No existe idempotencia POST | Revisión manual y advertencia; ningún reenvío automático incierto | Altas offline y primer envío | Garantía de exactamente una creación |
| Catálogo cambia durante paginación | Orden estable, detectar inconsistencias y conservar caché en fallo | Sincronización sin pérdida local | Snapshot remoto que el API no ofrece |

Parar solo el trabajo que depende del recurso faltante; mantener marcado pendiente y continuar lo independiente. No modificar backend o usar otra cuenta Firebase para ocultar el bloqueo. No publicar una app o enviar mensajes a terceros como parte de estas tareas.

## 9. Criterio de entrega y secuencia de comandos de cierre

Se ejecutan desde la raíz de trabajo elegida y cada comando conserva su salida:

```powershell
.\gradlew.bat :shared:testAndroidHostTest
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :androidApp:lintDebug
.\gradlew.bat :androidApp:connectedDebugAndroidTest
git diff --check
git status --short
git log --oneline -n 12
```

Añadir la tarea de verificación SQL cuyo nombre real quedó registrado en T00/T03. `connectedDebugAndroidTest` requiere AVD/dispositivo; no ejecutar pruebas destructivas contra datos personales. `git diff --check` debe no mostrar errores; antes de integrar, todos los archivos nuevos también deben haber sido revisados y estar incluidos en el diff staged apropiado.

El APK debug no es un build de distribución firmado para tienda. El reporte debe decir exactamente qué build se probó. No introducir contraseñas de keystore para completar un criterio de cierre de unidad que no pertenece a este alcance.

Definition of done:

- [ ] T00-T18 con resultado y enlaces de prueba; ninguna casilla marcada por expectativa.
- [ ] Los 14 criterios tienen evidencia o una limitación/pendiente explícito.
- [ ] Android realiza el flujo completo y los avisos con estado recuperable.
- [ ] Ninguna creación incierta se repite automáticamente.
- [ ] No hay pérdida local por migración, descarga parcial o cambio de cuenta.
- [ ] Firebase real Android acreditado, o bloqueo claramente señalado sin darlo por cumplido.
- [ ] iOS preparado y su validación pendiente identificada.
- [ ] Historial de commits por los siete pasos, informe y backlog sesión 14.

## 10. Revisión del plan y siguiente ejecución

Autoría del plan: no implica autorizar publicación ni cambios de alcance. Revisar el plan antes de implementarlo y elegir ejecución:

- Directa en esta sesión: un implementador conserva el contexto entre tareas y realiza revisión de conjunto al final. Recomendada para este plan porque sesión, SQL y coordinador comparten contratos y los cambios deben mantenerse coherentes.
- Con subagentes: implementador y revisor por tarea, seguido de revisión de conjunto. Mayor coste de contextos; útil si se prioriza revisión independiente frecuente.

No comenzar la implementación funcional solo porque el diseño fue aprobado: la petición actual era elaborar este plan. La elección de método y la instrucción de ejecutar habilitan la siguiente etapa.

## 11. Fuentes y vigencia

- Guía práctica local, 14 páginas: `C:/Users/USER/Downloads/sesion13_guia_practica_proyecto_integrador_mw9dtffakg.pdf`; páginas 3-4 (priorización y recorrido), 4-7 (SQL, sync y UI), 7-10 (permisos), 10-12 (Firebase/pruebas), 13 (entrega y cotejo).
- [SQLDelight 2.1.0: migraciones](https://sqldelight.github.io/sqldelight/2.1.0/android_sqlite/migrations/).
- [Android: permisos runtime](https://developer.android.com/training/permissions/requesting).
- [Android: notificaciones, incluidos permitir/denegar/deslizar](https://developer.android.com/develop/ui/compose/notifications/notification-permission).
- [GitLive Firebase Kotlin SDK y módulos](https://github.com/GitLiveApp/firebase-kotlin-sdk).
- [Código fuente de Crashlytics GitLive](https://github.com/GitLiveApp/firebase-kotlin-sdk/blob/master/firebase-crashlytics/src/commonMain/kotlin/dev/gitlive/firebase/crashlytics/crashlytics.kt); usar las APIs de la versión fijada, no asumir que master equivale a 2.7.0.
- [Firebase: puesta en marcha Crashlytics Android y plugins](https://firebase.google.com/docs/crashlytics/android/get-started).
- [Apple: permiso de notificaciones](https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications).

Las versiones candidatas se comprobaron en fuentes primarias para redactar el plan; su combinación con este proyecto todavía debe resolverse y compilarse en T15. Los comandos y resultados de ejecución se registran en el informe, no se deducen de este documento.

## 12. Autorrevisión de planificación

- Cobertura: secciones 1-4 del diseño -> restricciones/T00/T01; sesión y aislamiento -> T02/T03/T10; persistencia -> T03/T04; sincronización/revisión -> T05-T09; UI -> T11; permisos/avisos -> T12-T14; Firebase -> T15-T16; verificación y entrega -> T17-T18.
- Consistencia: los repositorios de cuenta reciben contexto inmutable en constructor; no se mezclan métodos con cuenta opcional. Descargar completo significa una pasada sin backoff en remoto, y cinco pasadas como cadena en el caso de uso.
- Review Focus: cada uno de los cinco casos tiene tarea y prueba explícita; el caso de excepción con PII también cubre causa/suppressed, no solo el mapa.
- Proporción: contiene contratos, aserciones y decisiones, no cuerpos completos de implementación. Las tareas de configuración y documentación se incluyen en la funcionalidad que acreditan.
- Límites: sin backend, sin push, sin prueba iOS ficticia, sin garantía POST imposible. Recursos externos no se sustituyen por stubs presentados como integración real.

Estado del documento: plan completo para revisión del usuario; ejecución funcional aún no iniciada.
