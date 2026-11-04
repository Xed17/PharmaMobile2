# Matriz de Cumplimiento de Criterios y Evidencias — Sesión 13

**Rama de entrega:** `feature/integrador-13`  
**Dispositivo de ejecución de pruebas:** Emulador Android Google Pixel 7 (API 34, Android 14)  
**Host de compilación:** Windows 11 — JDK 21 — Gradle 9.1.0  
**Backend de referencia:** PharmaSoft (Spring Boot 3.3.4, puerto 8080)  

---

## 1. Matriz de los 14 Criterios de Evaluación de la Guía

| N.º | Criterio de la Guía Práctica | Estado | Tarea / Commit | Evidencia Técnica Verificable |
|:---:|---|:---:|:---:|---|
| **1** | Recorrido completo del flujo móvil y tabla de huecos | **CUMPLE** | Paso 1 (`21ba2a3`) | Documento `docs/sesion13/01-recorrido-y-huecos.md` con los 8 escenarios (R1-R8), gravedades y decisiones cerradas. |
| **2** | Intentos y último error por fila en la base local | **CUMPLE** | Paso 2 (`aaf9146`) | Columnas en tabla `producto`: `intentos INTEGER`, `revision INTEGER`, `ultimoError TEXT`, `ultimoErrorCodigo TEXT`. Tests `ColaProductoLocalTest`. |
| **3** | Distinción de errores transitorios y permanentes | **CUMPLE** | T01 (`f22244d`) | Dominio `PoliticaReintento`: errores de red/timeout reintentables; 401/403/422/conflictos permanentes o de revisión. Tests `PoliticaReintentoTest`. |
| **4** | Espera creciente determinista y límite de intentos | **CUMPLE** | T01 (`f22244d`) / T08 (`c232665`) | Backoff exponencial truncado con límite máximo de 5 intentos y preservación de presupuesto de reintento. Pruebas con tiempo virtual en `CoordinadorSincronizacionTest`. |
| **5** | Agotamiento visible y acciones de recuperación | **CUMPLE** | T09 (`26eb5c0`) / Paso 4 (`b657014`) | Pantalla `RevisionEnviosScreen` con botones «Descartar» y «Reenviar con aceptación de riesgo». Captura `evidencias/E16-revision-envios.png`. |
| **6** | Subida antes de bajada sin pérdida de datos | **CUMPLE** | Paso 3 (`bd019d6`) | `CoordinadorSincronizacion` asegura subida de la cola de pendientes antes de reemplazar catálogo con descarga paginada. Pruebas `SincronizadorPrueba` y `CoordinadorSincronizacionTest`. |
| **7** | Pendientes, fallidas y antigüedad observables en UI | **CUMPLE** | Paso 4 (`b657014`) | `SincronizacionViewModel` emite `SincronizacionUiState` conectado a `ResumenSincronizacionCard` en `InicioScreen`. Captura real: `evidencias/E11-resumen-sincronizacion.png`. |
| **8** | Contrato común de permisos en lógica compartida | **CUMPLE** | Paso 5 (`a6c48b8`) | `domain/port/GestorPermisos.kt` con enum `Permiso.NOTIFICACIONES` y estados sellados `EstadoPermiso.Concedido`, `Denegado`, `DenegadoParaSiempre`. |
| **9** | Tres estados de permisos resueltos en Android | **CUMPLE** | Paso 5 (`a6c48b8`) | `GestorPermisosAndroid` + `PuentePermisosActivity`. Capturas en emulador real API 34 de los tres estados: `E12-permiso-denegado.png`, `E12-permiso-concedido.png`, `E12-permiso-ajustes.png`. |
| **10** | Declaración en manifiesto y claves de plataforma | **CUMPLE** | Paso 5 (`a6c48b8`) | `AndroidManifest.xml` con `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`. Preparación iOS en `docs/sesion13/06-verificacion-ios-pendiente.md`. |
| **11** | Explicación previa no invasiva y redirección a Ajustes | **CUMPLE** | Paso 5 (`a6c48b8`) | Banner informativo en `ResumenSincronizacionCard` con «Activar» y «Ahora no». Si es `DenegadoParaSiempre`, botón «Abrir Ajustes» redirige a configuración de la app en SO. |
| **12** | Integración de Firebase Crashlytics en capa de datos | **CUMPLE** | Paso 6 (`3539165`) | Interfaz de dominio `Telemetria` desacoplada del SDK. Implementación `TelemetriaFirebase` en `androidMain` envuelta por `TelemetriaSegura` (sanitiza excepciones, evita fuga de PII). |
| **13** | Pruebas de sincronización y permisos en `commonTest` | **CUMPLE** | Suites TDD | 116 pruebas ejecutadas en `:shared:testAndroidHostTest` con 0 fallos. Reporte JUnit en `shared/build/test-results/testAndroidHostTest/`. |
| **14** | Commits convencionales distribuidos por cada paso | **CUMPLE** | Pasos 1 a 7 | Historial lineal en `feature/integrador-13` con un commit por paso del entregable. |

---

## 2. Catálogo de Evidencias Capturadas en Android (AVD Pixel 7)

| Identificador | Archivo de Evidencia | Descripción del Estado Verificado |
|---|---|---|
| **E00** | [`evidencias/E00-baseline-inicio.png`](file:///C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2/docs/sesion13/evidencias/E00-baseline-inicio.png) | Línea base del inicio de la aplicación antes de las ampliaciones. |
| **E11** | [`evidencias/E11-resumen-sincronizacion.png`](file:///C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2/docs/sesion13/evidencias/E11-resumen-sincronizacion.png) | Pantalla de inicio con tarjeta `ResumenSincronizacionCard`: Pendientes: 1, En revisión: 1, antigüedad del catálogo, botones «Sincronizar» y «Revisar envíos». |
| **E12-D** | [`evidencias/E12-permiso-denegado.png`](file:///C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2/docs/sesion13/evidencias/E12-permiso-denegado.png) | **Estado 1 (Denegado / Inicial):** Banner voluntario visible con texto explicativo y botones «Activar» y «Ahora no». |
| **E12-C** | [`evidencias/E12-permiso-concedido.png`](file:///C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2/docs/sesion13/evidencias/E12-permiso-concedido.png) | **Estado 2 (Concedido):** Permiso otorgado en el SO; el banner desaparece automáticamente y las notificaciones locales quedan activas. |
| **E12-A** | [`evidencias/E12-permiso-ajustes.png`](file:///C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2/docs/sesion13/evidencias/E12-permiso-ajustes.png) | **Estado 3 (Denegado Permanente):** Permiso denegado con directiva `USER_FIXED`; el banner presenta el botón «Abrir Ajustes» para guiar al usuario a la configuración del sistema. |
| **E15** | [`logs/firebase-crashlytics.log`](file:///C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2/docs/sesion13/logs/firebase-crashlytics.log) | Logcat real de Android: `Initializing Firebase Crashlytics 19.4.0 for pe.edu.upeu.pharmamobile2`. |
| **E16** | [`evidencias/E16-revision-envios.png`](file:///C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2/docs/sesion13/evidencias/E16-revision-envios.png) | Pantalla `RevisionEnviosScreen` detallando error HTTP 422 persistido, intentos agotados y acciones individuales por fila. |

---

## 3. Estado de la Plataforma iOS (Criterio de Honestidad Técnica)

- **Condición de ejecución:** Host de desarrollo Windows 11 sin entorno Xcode nativo.
- **Implementación de código:** Adaptadores KMP completos en `shared/src/iosMain` (`GestorPermisosIos`, `NotificadorIos`, `TelemetriaIos`).
- **Estado formal:** Registrado como `PREPARADO / NO EJECUTADO EN MAC`. Ninguna captura o log de iOS ha sido inventado. El procedimiento de validación para entornos macOS se encuentra documentado en `docs/sesion13/06-verificacion-ios-pendiente.md`.
