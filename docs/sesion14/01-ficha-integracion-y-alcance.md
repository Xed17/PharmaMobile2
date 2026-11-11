# Sesión 14 · Paso 1 — Ficha de integración y congelación del alcance

> Documento editable. Los campos entre `[[ ]]` son para que los completes con tus datos.

| Dato | Valor |
|---|---|
| Estudiante / Equipo | [[Nombre y apellido]] · Equipo [[N]] |
| Proyecto | PharmaMobil (KMP) sobre PharmaSoft |
| Rama de trabajo | `feature/builds-14` (desde `develop`) |
| Fecha de congelación | [[dd/mm/2026]] |
| Versión a entregar | `1.0.0` (`versionCode` 1) |

## 1. Clasificación de huecos (viene de `docs/sesion13/01-recorrido-y-huecos.md`)

Tres decisiones posibles: **Cerrado**, **Se cierra hoy**, **Se declara como limitación**.
Lo "descartado" (ideas fuera del alcance) va en la sección 3 como trabajo futuro.

| ID | Recorrido | Decisión | Evidencia / motivo |
|---|---|---|---|
| R1 | Primera apertura (sin identidad ficticia offline) | **Cerrado** | `ContinuidadSesionTest` |
| R2 | Login inválido / sin red | **Cerrado** | `SesionViewModel`, banner con reintento |
| R3 | Caché vacía | **Cerrado** | Botones «Recargar» y «Reintentar descarga» |
| R4 | Alta offline | **Cerrado** | `GuardadoLocalInmediatoTest` |
| R5 | Reconexión sin duplicados | **Cerrado** | `PoliticaReintentoTest`, pantalla de revisión |
| R6 | Validación remota por fila | **Cerrado** | `EnvioPendientesTest` |
| R7 | Borrador al expirar la sesión | **Cerrado** | `BorradorProductoTest` |
| R8 | Aislamiento por cuenta | **Cerrado** | `AislamientoCuentaTest`, `ScopesCuentaTest` |
| R9 | Notificar en cada sincronización, aunque no haya novedades | **Cerrado** | Corregido en la auditoría: el coordinador usa `PoliticaAvisos` |
| R10 | HTTP permitido globalmente en release | **Cerrado** | Auditoría: `network_security_config` (ver paso 3) |

> Nota: R1–R8 se verificaron en emulador y en la suite automatizada. **Falta reverificarlos
> en dispositivo real** (paso 7); si alguno falla se registra como defecto, no se reabre aquí.

### Se cierra hoy (antes de firmar)

| Ítem | Qué se hace | Estado |
|---|---|---|
| URL del backend en teléfono real | Hacerla configurable (hoy está fija en `10.0.2.2`, solo válida en el emulador) | [[ ]] |
| Reglas de R8 | Escribir `proguard-rules.pro` y comprobar el release minificado (paso 5) | [[ ]] |
| `versionName` | Pasar de `1.0` a `1.0.0` | [[ ]] |
| Nombre visible de la app | Confirmar el nombre definitivo (hoy `PharmaMobile2`) | [[ ]] |
| README | Documentar `keystore.properties` sin contraseñas | [[ ]] |

### Se declara como limitación (para el informe)

| Limitación | Motivo | Qué haría falta para cerrarla |
|---|---|---|
| Clientes y Pedidos son una demostración en memoria (`InMemoryRepository`) | Quedaron fuera del alcance integrado en la sesión 13; está en el backlog T14-01 / T14-02 | `ClienteApi` y `VentaApi` con Ktor, tablas SQLDelight con migración y pruebas en `commonTest` |
| Sin build ni verificación en iOS | No se dispone de un dispositivo iOS ni de macOS con Xcode (paso 6 no ejecutado) | Mac con Xcode 16+, cuenta de Apple y un iPhone; ejecutar el paso 6 y `iosSimulatorArm64Test` |
| Solo 2 dispositivos Android probados en clase | El tercero se completa en la actividad autónoma | Probar en un tercer equipo (idealmente gama baja) |
| Solo Android verificado en dispositivo real | Consecuencia de la limitación de iOS | Igual que la de iOS |

## 2. Congelación del alcance

Desde la fecha de congelación **no entra**: funcionalidad nueva, cambios de arquitectura o de
librerías, rediseños de pantalla ni migraciones del esquema de la base.
Solo entran: correcciones de defectos que rompan el recorrido principal, textos y reglas de build.

Firmado: [[Nombre]] — [[Fecha]]

## 3. Trabajo futuro (descartado del alcance, no es un pendiente)

- Sustituir `androidx.security:security-crypto` (alfa y deprecada) por Keystore + DataStore.
- Migrar a versiones estables de `material3` y `lifecycle` (hoy alfa y beta).
- Mover `InMemoryRepository` y las pantallas de Clientes/Pedidos a ViewModels con repositorio.
- Pasar `RevisionEnviosScreen` a un ViewModel (hoy observa el repositorio desde el composable).
- Sustituir las APIs deprecadas de Compose (`menuAnchor`, `ScrollableTabRow`).
