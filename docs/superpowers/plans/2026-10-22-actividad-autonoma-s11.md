# Actividad Autónoma Sesión 11 — Offline-First, Suite de Pruebas y Sustentación U2

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Desarrollar, verificar y documentar todos los entregables de la Actividad Autónoma de la Sesión 11 para PharmaMobile2, cumpliendo con los 20 puntos de la rúbrica y generando el PDF final «S11_ActividadAutonoma_Chambilla.pdf».

---

### Task 1: Implementar la persistencia y presentación de la Antigüedad de Datos (Producto 3)

- [ ] **Step 1:** Agregar la tabla `Metadato` en `shared/src/commonMain/sqldelight/pe/edu/upeu/pharmamobil/db/Producto.sq`:
  ```sql
  CREATE TABLE IF NOT EXISTS Metadato (
      clave TEXT NOT NULL PRIMARY KEY,
      valor TEXT NOT NULL
  );

  obtenerMetadato:
  SELECT valor FROM Metadato WHERE clave = :clave;

  guardarMetadato:
  INSERT OR REPLACE INTO Metadato(clave, valor) VALUES (:clave, :valor);
  ```
- [ ] **Step 2:** Ejecutar la tarea Gradle para regenerar la interfaz de `PharmaDb`:
  Command: `./gradlew :shared:generateCommonMainPharmaDbInterface`
- [ ] **Step 3:** En `ProductoLocal.kt`, agregar métodos para leer y actualizar la marca de tiempo de sincronización:
  - `fun obtenerUltimaSincronizacion(): Long?`
  - `fun guardarUltimaSincronizacion(timestampMs: Long)`
- [ ] **Step 4:** Crear función utilitaria de formato en lenguaje natural en `commonMain`:
  - `formatearAntiguedad(timestampMs: Long?, ahoraMs: Long): String`
    - `null` -> `"Nunca sincronizado"`
    - `< 60_000` -> `"Actualizado hace un momento"`
    - `< 3_600_000` -> `"Actualizado hace X minutos"`
    - `< 86_400_000` -> `"Actualizado hace X horas"`
    - `>= 86_400_000` -> `"Actualizado hace X días"`
- [ ] **Step 5:** En `ProductoRepositoryOffline.kt`, actualizar la marca de tiempo tras una sincronización exitosa:
  - `local.guardarUltimaSincronizacion(obtenerTiempoActualMs())`
  - Agregar función para simulación de depuración: `suspend fun simularUltimaSincronizacionAntigua()`
- [ ] **Step 6:** En `ProductoViewModel.kt` y `ProductoUiState.kt`, exponer `ultimaSincronizacionTexto: String` y acción `simularSincronizacionAntigua()`.
- [ ] **Step 7:** En `ProductoScreen.kt`, renderizar el texto de antigüedad en la barra superior junto con un chip o botón de depuración para alternar a fecha antigua (>24 horas).
- [ ] **Step 8:** Commit atómico:
  `feat: registrar antiguedad de sincronizacion y formatear en lenguaje natural`

---

### Task 2: Habilitar Acceso Offline Resiliente si Existe Sesión Previa (Escenario 5)

- [ ] **Step 1:** En `AuthRepositorioRest.kt`, mejorar `recuperarSesion()` para que ante excepciones de red (`ConnectException` / `SinConexion` / `SocketTimeoutException`), si `almacenSeguro.obtenerToken()` existe, retorne una sesión de respaldo offline en lugar de enviar forzosamente a la pantalla de Login:
  - Construir `Sesion(usuario = "Usuario (Offline)", roles = listOf("ROLE_ADMIN"), token = token)`.
- [ ] **Step 2:** Verificar que `SesionViewModel` transicione a `EstadoSesion.Autenticada` permitiendo ver los productos cacheados en SQLite local mientras se esté en modo avión.
- [ ] **Step 3:** Al reconectar internet, `sincronizar()` valida las credenciales contra el servidor; si el token expiró (401), ejecuta `notificar401()` y cierra la sesión limpiamente.
- [ ] **Step 4:** Commit atómico:
  `feat: permitir acceso a catalogo local offline con token almacenado`

---

### Task 3: Suite Completa de 6 Pruebas Unitarias en `commonTest` (Producto 1)

- [ ] **Step 1:** En `shared/src/commonTest/kotlin/pe/edu/upeu/pharmamobile2/ProductoRepositoryOfflineTest.kt`, estructurar las 6 pruebas solicitadas de forma independiente (cada una con su propia base en memoria mediante `baseEnMemoria()`):
  1. `test1_sincronizacionConRed_flowEmiteListaCompleta`
  2. `test2_registroSinRed_guardaPendienteYFlowEmite` (Esencial)
  3. `test3_envioAlReconectar_eliminaMarcaPendiente`
  4. `test4_laBajadaNoBorraLoPendiente_conservaCreadosSinRed` (Esencial)
  5. `test5_identificadoresSinColision_convivenSinSobrescribirse`
  6. `test6_sincronizarDosVecesNoDuplica_mismaCantidadFilas`
- [ ] **Step 2:** Ejecutar la suite completa y capturar la salida de Gradle:
  Command: `./gradlew :shared:testAndroidHostTest`
- [ ] **Step 3:** Confirmar que las 6 pruebas pasen con 100% de éxito.
- [ ] **Step 4:** Commit atómico:
  `test: implementar suite completa de seis pruebas offline en commonTest`

---

### Task 4: Bitácora de los 5 Escenarios Offline y Generación de Evidencias (Producto 2)

- [ ] **Step 1:** Desplegar y probar en emulador/dispositivo los 5 escenarios:
  1. **Apertura sin red con datos previos:** Sincronizar catálogo, cerrar app, activar modo avión y reabrir. Verificar listado visible y aviso de sin conexión.
  2. **Registro sin red:** En modo avión, registrar producto nuevo. Verificar badge `⏳ Pendiente` e ID negativo.
  3. **Reconexión y envío:** Desactivar modo avión y sincronizar. Verificar en Swagger `GET /api/v1/productos` con ID positivo del servidor.
  4. **Primera apertura sin red:** Borrar datos de almacenamiento, activar modo avión y abrir. Verificar estado vacío `SinProductos` informativo (sin crash ni pantalla de error).
  5. **Sesión expirada sin red:** Abrir con token en modo avión. Verificar acceso a datos locales y resolución de sesión al volver la red.
- [ ] **Step 2:** Capturar las pantallas correspondientes a cada escenario mediante ADB / capturas del emulador.
- [ ] **Step 3:** Documentar para cada escenario:
  - Identificador y nombre
  - Fecha y hora
  - Plataforma
  - Pasos exactos seguidos
  - Qué mostró la aplicación
  - Qué había en la base local SQLite
  - Imagen de captura
  - Conclusión analítica en una línea

---

### Task 5: Redacción del Guion de Sustentación del Producto U2 (Producto 4)

- [ ] **Step 1:** Diseñar la estructura minuto a minuto (8 minutos totales) para la sustentación del 27 de octubre:
  - **Minuto 0–1:** Autenticación JWT (`admin` y credenciales inválidas).
  - **Minuto 1–3:** Consumo REST y CRUD completo (Listar, registrar, actualizar).
  - **Minuto 3–5:** Manejo de estados y errores (403 con usuario `vendedor` y error 400 de validación).
  - **Minuto 5–7:** Persistencia offline-first (Modo avión, badge pendiente, reconexión y reconciliación atómica).
  - **Minuto 7–8:** Evidencia multiplataforma (Android e iOS compartiendo arquitectura).
- [ ] **Step 2:** Incluir los usuarios asignados para cada bloque (`admin`, `vendedor`, `almacen`), los datos previos preparados en la base de datos y el estado de PharmaSoft.

---

### Task 6: Generación del Documento Final PDF «S11_ActividadAutonoma_Chambilla.pdf»

- [ ] **Step 1:** Crear script Python automatizado `generate_s11_document.py` con `python-docx` y exportación a PDF vía Word (`win32com.client`).
- [ ] **Step 2:** Incluir:
  - Portada institucional UPeU completa (estudiante Eduard Chambilla, fecha, curso, enlace a rama).
  - Producto 1: Suite de pruebas en `commonTest` (tabla, código y salida completa de consola).
  - Producto 2: Bitácora de 5 escenarios offline con capturas integradas y análisis.
  - Producto 3: Antigüedad de datos en pantalla (explicación técnica, capturas reciente y >24h).
  - Producto 4: Guion de sustentación de 8 minutos para el 27 de octubre.
  - Sección 5: Lista de cotejo de los 11 puntos verificados.
- [ ] **Step 3:** Ejecutar el script y compilar el archivo PDF final:
  `S11_ActividadAutonoma_Chambilla.pdf`
- [ ] **Step 4:** Commit atómico y push al repositorio remoto:
  `docs: generar informe y evidencias de la actividad autonoma sesion 11`
