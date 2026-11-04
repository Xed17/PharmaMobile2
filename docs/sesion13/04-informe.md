# Informe Técnico Final — Sesión 13: Proyecto Integrador Móvil

**Aplicación:** PharmaMobile2 (Kotlin Multiplatform + Compose Multiplatform)  
**Backend:** PharmaSoft (Spring Boot 3.3.4, puerto 8080)  
**Rama:** `feature/integrador-13`  
**Fecha:** 4 de noviembre de 2026  
**Entorno de Verificación:** Android Emulator Pixel 7 (API 34, Android 14) / Host Windows 11 JDK 21  

---

## 1. Resumen Ejecutivo y Objetivos

La **Sesión 13 (Proyecto Integrador)** tuvo como propósito consolidar la robustez industrial de la aplicación móvil `PharmaMobile2` en escenarios de conectividad intermitente (*Offline-First*), incorporando sincronización recuperable, persistencia resiliente, gestión de permisos nativos en tiempo de ejecución y telemetría de fallos con Firebase Crashlytics.

Todos los entregables requeridos por la guía y la especificación técnica han sido completados en la rama `feature/integrador-13` con commits convencionales trazables para cada paso, acompañados de 116 pruebas automatizadas con 0 fallos y capturas reales de ejecución en dispositivo Android.

---

## 2. Arquitectura de las Soluciones Implementadas

### 2.1 Esquema Local Ampliado y Aislamiento Multicuenta (Pasos 2 y 3)
- **Migración v2 de SQLDelight:** Se rediseñaron las tablas `producto`, `metadato` y la nueva tabla `borrador_producto`.
- **Clave compuesta por cuenta:** Se incorporó `cuentaClave TEXT NOT NULL` en la clave primaria `(cuentaClave, id)`, garantizando que múltiples usuarios puedan operar en el mismo dispositivo físico sin contaminación cruzada de datos, catálogos ni colas de envío.
- **Trazabilidad de sincronización por fila:** La tabla `producto` almacena `intentos INTEGER`, `revision INTEGER`, `estadoEnvio TEXT` (`CONFIRMADO`, `PENDIENTE`, `ENVIANDO`, `REVISION`, `DESCARTADO`), `ultimoErrorCodigo TEXT` y `ultimoError TEXT`.

### 2.2 Política de Reintentos Seguros frente a POST no idempotente
- **Protección contra duplicados:** En HTTP REST, el método `POST` de alta de productos no implementa clave de idempotencia en el servidor. Un fallo de conexión durante la respuesta podría significar que el producto ya fue creado en la base de datos remota.
- **Regla estricta:** Solo se reintentan automáticamente las operaciones seguras e idempotentes (`GET`). Cuando un `POST` experimenta un fallo ambiguo de red o supera el límite de 5 intentos con backoff exponencial, pasa de forma determinista a estado `REVISION`.
- **Resolución asistida:** Se implementó `RevisionEnviosScreen`, permitiendo al usuario inspeccionar el motivo del fallo (por ejemplo, `422 Unprocessable Entity`) y decidir individualmente si desea «Descartar» o «Reenviar aceptando el riesgo de duplicación».

### 2.3 Coordinador de Sincronización y Descarga Atómica
- **Secuencia garantizada:** El `CoordinadorSincronizacion` sube primero la cola local de pendientes antes de proceder a la descarga remota.
- **Paginación completa:** La descarga recupera la totalidad de los productos paginados del backend (en bloques de 20 hasta agotar páginas) en lugar de truncar en la primera página.
- **Transaccionalidad:** El reemplazo del catálogo sincronizado, el guardado de la marca de tiempo y el estado del ciclo se consolidan en una única transacción SQLite atómica.

### 2.4 Pantalla Principal con Estado de Sincronización Observable (Paso 4)
- **Componente `ResumenSincronizacionCard`:** Ubicado en la parte superior de `InicioScreen`, expone mediante flujos reactivos (`StateFlow`):
  - Número de cambios pendientes de subida (`Pendientes`).
  - Número de filas fallidas o en conflicto (`Fallidas` / `En revisión`).
  - Antigüedad del catálogo en formato legible (*"Actualizado hace un momento"*).
  - Botones de acción contextual: «Sincronizar», «Revisar envíos» y «Reintentar descarga».

### 2.5 Gestor de Permisos Nativo y Notificaciones Locales (Paso 5)
- **Contrato Común (`commonMain`):** Interfaz `GestorPermisos` con enum `Permiso.NOTIFICACIONES` y estados sellados `EstadoPermiso.Concedido`, `EstadoPermiso.Denegado` y `EstadoPermiso.DenegadoParaSiempre`.
- **Implementación Android:** `GestorPermisosAndroid` serializa solicitudes concurrentes mediante un mutex y `CompletableDeferred`, integrándose con `PuentePermisosActivity` sobre el contrato de Activity Results de Jetpack.
- **Los Tres Estados de Permiso en UI:**
  1. *Denegado / Inicial:* La tarjeta muestra un banner voluntario con texto explicativo (*"Activa los avisos para saber si tus cambios se enviaron o necesitan revisión"*) y botones «Activar» y «Ahora no».
  2. *Concedido:* Al aceptar o conceder el permiso en el SO, el banner desaparece de inmediato y se habilita la publicación de notificaciones seguras en el canal `sincronizacion`.
  3. *Denegado Permanente (`DenegadoParaSiempre` / `USER_FIXED`):* Si el usuario marca «No volver a preguntar» o la política del SO impide nuevos diálogos, el banner ofrece el botón «Abrir Ajustes» que abre la configuración de la app en Android mediante `ACTION_APPLICATION_DETAILS_SETTINGS`.

### 2.6 Integración de Firebase Detrás de Interfaz de Dominio (Paso 6)
- **Desacoplamiento de SDK:** El módulo de dominio no importa paquetes de Firebase. Se definió el puerto `Telemetria` con métodos para registrar eventos y excepciones.
- **Sanitización Obligatoria (`TelemetriaSegura`):** Todo reporte pasa por un decorador que elimina tokens JWT, direcciones de correo electrónico o parámetros sensibles antes de delegar a `TelemetriaFirebase`. Asimismo, se filtran las excepciones `CancellationException` para no distorsionar las métricas con cancelaciones normales de corrutinas.
- **Configuración y Build:** Incorporación de `androidApp/google-services.json` correspondiente al proyecto registrado `pharmamobile2` (Project Number `472924621717`), plugin `com.google.gms.google-services` y resolución de build ID de Crashlytics.

---

## 3. Resultados de Pruebas y Validación Técnica

### 3.1 Suite de Pruebas KMP (`testAndroidHostTest`)
Se ejecutó la suite completa sobre el módulo compartido:
- **Total de pruebas ejecutadas:** 116 tests.
- **Fallos:** 0.
- **Errores:** 0.
- **Omitidas:** 0.
- **Resultado:** `BUILD SUCCESSFUL`.
- **Suites destacadas:**
  - `AislamientoCuentaTest`: Verifica separación absoluta de datos SQLite entre dos usuarios con los mismos IDs de producto.
  - `ContinuidadSesionTest`: Valida que el modo sin conexión requiera una sesión previamente autenticada en el servidor.
  - `PoliticaReintentoTest`: Valida backoff determinista y derivación de POST ambiguo a revisión.
  - `GestorPermisosAndroidTest`: Comprueba deduplicación de solicitudes concurrentes y mapeo de los tres estados.
  - `TelemetriaSeguraTest`: Comprueba sanitización de tokens y filtrado de PII.

### 3.2 Compilación del Artefacto de Aplicación
- Comando: `./gradlew :androidApp:assembleDebug`
- Resultado: `BUILD SUCCESSFUL`.
- Artefacto generado e instalado: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

---

## 4. Registro de Commits por Paso

El desarrollo fue organizado en la rama `feature/integrador-13` cumpliendo con la convención de un commit por paso del entregable:

1. **Paso 1:** `21ba2a3 docs(s13): registrar plan entorno y brechas verificadas`
2. **Paso 2:** `aaf9146 feat(local): migrar catalogo y persistir estado de envio por cuenta`
3. **Paso 3:** `bd019d6 feat(sync): cerrar caso de uso de sincronizacion y aislamiento de cuenta (paso 3)`
4. **Paso 4:** `b657014 feat(ui): presentar resumen de sincronizacion pendientes y fallidas en inicio (paso 4)`
5. **Paso 5:** `a6c48b8 feat(native): integrar gestor de permisos y notificaciones locales (paso 5)`
6. **Paso 6:** `3539165 feat(telemetria): integrar firebase crashlytics bajo interfaz de dominio (paso 6)`
7. **Paso 7:** `Commit de cierre docs(s13): registrar informe final y evidencias de ejecucion (paso 7)`

---

## 5. Criterio de Honestidad Técnica respecto a iOS

Conforme a las buenas prácticas de ingeniería de software y los lineamientos del proyecto:
- El entorno host disponible es Windows 11 con Android SDK.
- Las implementaciones para iOS se desarrollaron en `shared/src/iosMain` (`GestorPermisosIos`, `NotificadorIos`, `TelemetriaIos`).
- No se han simulado ni fabricado capturas de iPhone o logs de Xcode en Windows. El estado de la plataforma iOS queda documentado como `PREPARADO / NO EJECUTADO EN MAC` en `docs/sesion13/06-verificacion-ios-pendiente.md`.
