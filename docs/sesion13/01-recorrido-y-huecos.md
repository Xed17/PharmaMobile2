# Recorrido y huecos de sesión 13

Estado: auditoría estática realizada; recorrido REST real pendiente de backend y cuenta de prueba. No se presentan observaciones estáticas como capturas de ejecución.

Base: commit 531a48c, rama original feature/offline-first-chambilla. No existe develop local/remoto. Se creó feature/integrador-13 desde ese HEAD en worktree aislado, conservando la implementación offline existente.

| ID | Recorrido | Hallazgo comprobado en código | Gravedad | Decisión | Evidencia de ejecución |
|---|---|---|---|---|---|
| R1 | Primera apertura | Botón offline fabrica identidad administrador | Alta | Cerrar en T02 | Pendiente |
| R2 | Login inválido/sin red | Hay errores tipados de sesión; comprobar textos en dispositivo | Media | Verificar T18 | Pendiente backend |
| R3 | Caché vacía | Existe estado SinProductos; verificar recuperación | Media | Completar T11 | Pendiente |
| R4 | Alta offline | Persiste, pero registrar espera el intento HTTP | Alta | Cerrar T06 | Tests base verdes |
| R5 | Reconexión | POST sin idempotencia; descarga solo primera página | Crítica | T05-T09, revisión manual de inciertos | Pendiente backend |
| R6 | Validación | Una fila fallida detiene cola; falta error persistido | Alta | Cerrar T04/T07 | Pendiente |
| R7 | Expiración en formulario | Sin borrador persistente por cuenta | Alta | T02/T10/T11 | Pendiente |
| R8 | Salida/cambio de cuenta | Token se limpia, datos SQL sin propietario | Crítica | Migración y aislamiento T03/T10 | Pendiente |

Clientes y pedidos: actualmente demostraciones en memoria; integración REST en sesión 14. En sesión 13 se elimina cruce de estado entre cuentas y se identifica su condición de demostración.

Prueba inicial original: 47 tests, 0 fallos; assembleDebug exitoso. Ver logs/baseline-original.log. Emulador Pixel_7 arrancado temporalmente en modo read-only; backend localhost:8080/api/health agotó timeout de 3s.
