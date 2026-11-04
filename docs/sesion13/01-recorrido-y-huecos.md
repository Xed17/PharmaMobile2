# Recorrido y huecos de sesión 13

**Estado:** Verificación completa en emulador Android Pixel_7 (API 34) y suite de pruebas automatizadas KMP (116 tests pasando sin fallos).  
**Base:** Commit `531a48c`, rama original `feature/offline-first-chambilla`. Se creó la rama `feature/integrador-13` en worktree aislado, implementando los 7 pasos con commits convencionales.

---

## 1. Tabla de Recorridos y Huecos Evaluados

| ID | Recorrido | Hallazgo comprobado en código base | Gravedad | Decisión de Arquitectura | Evidencia de Ejecución | Estado |
|---|---|---|---|---|---|---|
| **R1** | Primera apertura | El botón offline fabricaba identidad administrador ficticia sin validación previa | Alta | Se eliminó la creación sintética. Continuidad offline restringida exclusivamente a usuarios autenticados previamente en el backend (`ContinuidadSesionTest`, T02). | Comprobado en emulador: mensaje *"Inicia sesión con conexión antes de continuar sin red"*. Pruebas `ContinuidadSesionTest` verdes. | **CERRADO** |
| **R2** | Login inválido / sin red | Errores tipados de sesión (`ErrorSesion.Red`, `CredencialesInvalidas`) sin feedback unificado | Media | Tratamiento centralizado en `SesionViewModel`. Banner contextual no invasivo con reintento. | Emulador: muestra *"No fue posible conectar con el servidor. Verifique su red"* en modo desconectado. | **CERRADO** |
| **R3** | Caché vacía | Estado `SinProductos` no permitía recuperar catálogo de forma explícita | Media | Incorporación de botón «Recargar» y «Reintentar descarga» atómico en `ResumenSincronizacionCard` y `ProductoScreen`. | Emulador: catálogo inicial con 15 productos reales cargados desde PharmaSoft (`E11-resumen-sincronizacion.png`). | **CERRADO** |
| **R4** | Alta offline | Registro guardaba pero bloqueaba la UI esperando intento HTTP síncrono | Alta | Guardado local transaccional inmediato con ID provisional y encolado asíncrono cancelable por cuenta (`ProductoRepositoryOffline.guardarPendiente`). | Pruebas `GuardadoLocalInmediatoTest` verdes. Fila encolada con estado `PENDIENTE` en `pharma.db`. | **CERRADO** |
| **R5** | Reconexión | POST sin idempotencia; el servidor podía crear pero fallar la respuesta, provocando duplicados al reintentar | Crítica | Política de reintentos seguros: reintento automático exclusivo para operaciones idempotentes (GET). Los fallos inciertos de POST pasan a estado `REVISION` para confirmación manual (`PoliticaReintentoTest`). | Pantalla `RevisionEnviosScreen` con opciones explícitas («Descartar», «Reenviar con aceptación de riesgo»). `E16-revision-envios.png`. | **CERRADO** |
| **R6** | Validación remota | Una fila rechazada por el backend (422) detenía todo el lote de subida | Alta | Procesamiento independiente por fila. El error HTTP/validación se persiste en `ultimoError` y `ultimoErrorCodigo`, marcando la fila como `REVISION` sin bloquear las demás (`EnvioPendientesTest`, T07). | Verificado en test suite (116 tests verdes) y en pantalla de revisión con error *"422 Unprocessable Entity: Lote duplicado"*. | **CERRADO** |
| **R7** | Expiración en formulario | Sin borrador persistente por cuenta si la app se cerraba o expiraba la sesión | Alta | Tabla `borrador_producto` aislada por clave de cuenta con clave única `cuentaClave` (`BorradorProductoTest`, T04). | Verificado en suite de tests `BorradorProductoTest` y `BorradorProductoViewModelTest`. | **CERRADO** |
| **R8** | Salida / cambio de cuenta | Token JWT se limpiaba pero SQLite no tenía aislamiento por cuenta, cruzando datos entre usuarios | Crítica | Migración v2 a esquema compuesto `(cuentaClave, id)`. Aislamiento criptográfico y lógico estricto por cuenta (`AislamientoCuentaTest`, `ScopesCuentaTest`, T03/T10). | Tests de aislamiento pasando. Consulta SQL verifica que los datos llevan clave `v1|27:pharmasoft-desarrollo-local|5:admin`. | **CERRADO** |

---

## 2. Alcance respecto a Clientes y Pedidos

- Los módulos de Clientes y Pedidos operan como demostraciones en memoria aisladas en esta sesión.
- Su integración formal con el backend REST de ventas de PharmaSoft forma parte del backlog de la Sesión 14.
- Se añadió identificación visible en la pantalla principal para que el evaluador conozca claramente su condición de módulo de demostración.
