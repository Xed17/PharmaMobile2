# Sesión 13: diagnóstico, requisitos y decisiones pendientes

Fecha de revisión: 4 de noviembre de 2026.

Estado: diagnóstico completo, cinco decisiones de alcance resueltas y diseño aprobado por el usuario. El plan completo está en `docs/superpowers/plans/2026-11-04-sesion13-integrador-implementation-plan.md`. No hay cambios funcionales en el código de la aplicación ni en el backend. Las brechas siguientes proceden de inspección estática; no equivalen a resultados de pruebas o recorridos en un dispositivo.

## 1. Fuente y alcance de la guía

Fuente: `C:/Users/USER/Downloads/sesion13_guia_practica_proyecto_integrador_mw9dtffakg.pdf`, Guía Práctica N.º 13, 14 páginas, fechada el 3 de noviembre de 2026.

La sesión corresponde al proyecto móvil PharmaMobil sobre el servicio PharmaSoft. En este equipo se han identificado estos proyectos:

| Parte | Ubicación | Papel |
| --- | --- | --- |
| Aplicación | `C:/Users/USER/Documents/ciclo 6/moviles/PharmaMobile2` | Android, iOS, UI y lógica común KMP |
| Backend | `C:/Users/USER/Documents/ciclo 6/moviles/pharmasoft/pharmaSoft` | Spring Boot, REST, autenticación y persistencia del servidor |

La petición actual se interpreta como preparar un plan de implementación detallado. La ejecución de cambios funcionales será una etapa posterior, cuando estén resueltas las decisiones que condicionan el diseño.

### 1.1 Lo exigido para esta sesión

1. Recorrer el flujo completo y registrar sus huecos con gravedad y decisión.
2. Conservar intentos y último error de cada registro pendiente en la base local.
3. Distinguir errores transitorios y permanentes; aplicar espera creciente y límite de intentos.
4. Mantener la secuencia de subir pendientes antes de conciliar la descarga.
5. Mostrar pendientes, fallidas y antigüedad de datos mediante estado observable.
6. Declarar el contrato de permisos en código común e implementar los tres estados en Android.
7. Usar un permiso real, explicar su propósito y ofrecer Ajustes cuando no pueda volver a solicitarse.
8. Integrar Firebase mediante una interfaz propia de dominio, con el SDK encapsulado en datos.
9. Probar sincronización y permisos falsos desde `commonTest`.
10. Entregar tabla de huecos, código, capturas y commits distribuidos por los siete pasos.

### 1.2 Alcance de unidad frente a alcance de sesión

La página 1 menciona Android/iOS, dispositivos reales, builds firmados y sustentación como criterios del producto de la unidad. Las páginas 3 y 13 concretan el trabajo y el entregable de esta sesión. El plan deberá separar:

- Requisitos explícitos del entregable de sesión 13.
- Correcciones previas necesarias para que esos requisitos funcionen.
- Trabajo de la sesión 14 o del cierre de unidad, con justificación.
- Ampliaciones elegidas por el usuario, sin atribuirlas a la guía.

No se desprende de esta guía que haya que añadir otro sistema de autenticación, trasladar el catálogo a Firestore o sustituir PharmaSoft. JWT y REST existentes siguen siendo parte de la base del proyecto.

## 2. Base existente comprobada en el código

| Área | Evidencia | Situación observada |
| --- | --- | --- |
| Multiplataforma | `shared/build.gradle.kts`, `androidApp/`, `iosApp/` | Targets Android, iOS ARM64 y simulador ARM64 |
| Presentación | `shared/src/commonMain/kotlin/pe/edu/upeu/pharmamobile2/presentation/` | Compose, ViewModels, estados y validadores |
| Arquitectura | `domain/`, `data/`, `di/AppModule.kt` | Contratos, casos de uso, repositorios y Koin |
| Red | `data/remote/HttpClientFactory.kt`, `ProductoApi.kt` | Ktor, JSON, JWT centralizado y tiempos de espera |
| Errores | `domain/error/ErrorApi.kt`, `data/remote/EjecutarLlamada.kt` | Validación, 401, 403, 404, conflicto, servidor, red y timeout |
| Persistencia | `Producto.sq`, `Metadato.sq`, `data/local/ProductoLocal.kt` | SQLite con SQLDelight; catálogo y fecha de sincronización |
| Offline | `data/repository/ProductoRepositoryOffline.kt` | Alta local con ID negativo, subida y confirmación transaccional |
| Reactividad | `ProductoLocal.observar()`, `ProductoViewModel` | La lista observa la base local |
| Estado de sincronización | `ProductoUiState.kt`, `ProductoViewModel.kt` | En curso, mensaje, total de pendientes y antigüedad |
| Pruebas | `shared/src/commonTest/` | Dominio, autenticación, ViewModels y repositorio offline |
| Clientes y pedidos | `data/InMemoryRepository.kt`, pantallas correspondientes | Registro principalmente en memoria; pedidos usan catálogo de ejemplo |

Importante: `PermisosTest.kt` comprueba permisos de negocio/RBAC y mapeo de sesión. No comprueba permisos nativos de cámara, ubicación o notificaciones.

La rama observada durante la revisión es `develop`. Existe una carpeta de imágenes sin seguimiento en `docs/superpowers/plans/extracted_images/`; no pertenece a este trabajo y debe preservarse.

## 3. Matriz de los 14 criterios de evaluación

Los estados de esta tabla indican evidencia estática, no cumplimiento certificado en ejecución.

| N.º | Criterio de la guía | Estado observado | Trabajo o comprobación pendiente |
| --- | --- | --- | --- |
| 1 | Recorrido completo y tabla de huecos | Pendiente de recorrido real | Ejecutar los ocho escenarios; registrar gravedad, evidencia y decisión |
| 2 | Intentos y último error por fila | Falta | Migración, consultas, modelos de lectura y escritura |
| 3 | Errores transitorios y permanentes | Parcial | Existe `ErrorApi`, falta su traducción a resultado de envío |
| 4 | Espera creciente y máximo de intentos | Falta | Política determinista, persistencia y pruebas con tiempo virtual |
| 5 | Agotamiento visible | Falta | Estado fallido, motivo y acciones de recuperación |
| 6 | Subida antes de bajada sin perder datos | Parcial | Existe el orden; falta resolver duplicados y descarga paginada completa |
| 7 | Pendientes, fallidas y antigüedad | Parcial | Ya hay pendientes y antigüedad; faltan fallidas y actualización temporal |
| 8 | Contrato común de permisos | Falta | `GestorPermisos`, `Permiso`, `EstadoPermiso` |
| 9 | Tres estados Android | Falta | Implementación, historial de solicitud y puente con Activity |
| 10 | Manifiesto y claves iOS aplicables | Falta para capacidades nuevas | Declarar únicamente el permiso elegido y configurar cada plataforma |
| 11 | Explicación previa y acción Ajustes | Falta | Flujo contextual, cancelación y actualización al volver a la app |
| 12 | Firebase solo en datos | Falta integración | Contrato, adaptador, inicialización y evidencia real del proveedor |
| 13 | Pruebas de `commonTest` pasando | Hay pruebas; no ejecutadas en esta revisión | Obtener línea base y añadir escenarios de sesión 13 |
| 14 | Commits convencionales por paso | Trabajo futuro | Preparar rama de la guía y siete hitos comprobables |

La guía propone `feature/integrador-13` desde `develop`. Se registra como requisito de entrega; la lectura del PDF no ha provocado un checkout, commit o push automático.

## 4. Brechas concretas y prioridades propuestas

Prioridad crítica: puede duplicar, perder, exponer o atribuir incorrectamente datos.
Prioridad alta: impide un criterio obligatorio o la recuperación del flujo.
Prioridad media: afecta claridad, verificabilidad o mantenibilidad.

| ID | Prioridad | Hallazgo | Consecuencia | Evidencia principal |
| --- | --- | --- | --- | --- |
| H01 | Crítica | El `POST` de producto no lleva clave de operación y no se encontró soporte de idempotencia en el backend inspeccionado | Si el servidor crea pero se pierde la respuesta, reenviar puede duplicar | `ProductoApi.crear()`, controlador y servicio de productos del backend |
| H02 | Crítica | `obtenerProductos()` devuelve solo `listar().contenido` con tamaño por defecto 20 | La conciliación borra sincronizados y repone solo la primera página; el catálogo local puede quedar incompleto | `ProductoApi.kt`, `ProductoLocal.reemplazarSincronizados()` |
| H03 | Alta | La tabla de productos guarda solo ID, nombre, precio, stock y pendiente | Se pierde categoría y estado activo al pasar por almacenamiento local | `Producto.sq`, `ProductoLocal.toDomain()` |
| H04 | Alta | `toDomain()` fuerza activo y categoría de ejemplo | Productos inactivos pueden aparecer activos y se puede enviar una categoría incorrecta | `ProductoLocal.kt` |
| H05 | Alta | El repositorio detiene la sincronización con la primera excepción de una fila | Una fila inválida puede bloquear las demás | `ProductoRepositoryOffline.sincronizar()` |
| H06 | Alta | No hay intentos, último error ni fallidas persistidas | No se puede limitar ni explicar el reintento | `Producto.sq`, `ProductoUiState.kt` |
| H07 | Alta | `registrar()` espera a `sincronizar()` aunque el comentario diga segundo plano | Guardar en modo sin red puede esperar el timeout antes de completar el flujo | `ProductoRepositoryOffline.registrar()` |
| H08 | Alta | Hay capturas genéricas de `Exception` y `runCatching` en flujos suspendidos | Pueden consumir cancelaciones y traducirlas como un fallo normal | Repositorios offline y de autenticación; `ProductoViewModel` |
| H09 | Alta | El acceso offline crea una sesión local con rol administrador | Debe definirse si es demostración o acceso permitido; no equivale a recuperar una identidad validada | `SesionViewModel.accederModoOffline()` |
| H10 | Crítica | Al salir se borra el token, pero no se observa aislamiento de pendientes por usuario en el esquema | Debe impedirse que una cuenta vea o envíe cambios creados por otra | `AuthRepositorioRest.cerrarSesion()`, `Producto.sq` |
| H11 | Alta | La expiración conduce al login; no se observa un contrato de borrador recuperable del formulario | Hay que comprobar y decidir cómo conservar lo escrito sin mezclar usuarios | `SesionViewModel`, `ProductoViewModel`, `App.kt` |
| H12 | Media | La antigüedad se recalcula al emitir el timestamp, sin reloj observable | Puede mostrar un texto antiguo mientras la pantalla permanece abierta | `iniciarObservacionSincronizacion()` |
| H13 | Alta | No hay gestor nativo, manifiesto de nuevos permisos ni integración Firebase | Faltan los pasos 5 y 6 completos | AndroidManifest, módulos Koin, Gradle e Info.plist |
| H14 | Media | No se observa ejecución automática al recuperar conectividad | La reconexión necesita un disparador definido y no debe enviar el mismo registro dos veces | `ProductoViewModel` y repositorio offline |
| H15 | Media | Inicio no expone el resumen de sincronización que existe parcialmente en Productos | Debe acordarse dónde estará visible el estado principal | `presentation/inicio/InicioScreen.kt`, `ProductoScreen.kt` |
| H16 | Media | Clientes y pedidos no comparten el catálogo persistido real | El recorrido de integración debe documentar el límite o planear su cierre | `InMemoryRepository.kt`, pantallas de clientes y pedidos |

Estas prioridades son propuestas del análisis, no decisiones aceptadas por el usuario. H09, H10, H11 y H16 necesitan definir comportamiento y alcance antes de comprometer una implementación.

## 5. Los ocho recorridos que se deberán ejecutar

| Escenario | Preparación | Resultado que se deberá comprobar | Evidencia |
| --- | --- | --- | --- |
| Primera apertura sin sesión | Instalación nueva | Login comprensible y acciones disponibles | Captura y registro de resultado |
| Credenciales incorrectas | Backend accesible | Mensaje distinto de falta de conexión | Captura del error |
| Base local vacía | Sin productos locales | Estado vacío propio, sin bloqueo ni falso error | Captura |
| Crear sin red | Sesión según política acordada, modo avión | Persistencia inmediata, marca pendiente y mensaje preciso | Captura y comprobación tras reinicio |
| Reconexión | Una o más filas pendientes | Confirmación sin duplicados y actualización observable | Captura móvil y comprobación servidor |
| Validación servidor | Payload rechazado de forma controlada | Error asociado a fila/campo y demás envíos capaces de avanzar | Registro de error y recuperación |
| Token vencido en formulario | Expirar/revocar token en entorno de prueba | Aviso y recuperación del borrador según política acordada | Capturas antes/después |
| Cerrar sesión y cambiar usuario | Cuenta A con pendientes, luego cuenta B | Token anterior ausente y datos/cola correctamente aislados | Registro de las dos sesiones |

No se declarará realizado ningún recorrido sin evidencia. Las pruebas automatizadas complementarán estos recorridos y no sustituirán los diálogos reales del sistema ni la verificación en Firebase.

## 6. Detalles de la guía que requieren adaptación

1. Sus fragmentos SQL usan `labor`, mientras que este proyecto usa `producto`: no copiar esos nombres literalmente.
2. El dominio ya llama `SinPermisos` al caso 403; el ejemplo de la guía usa `SinPrivilegio`. Mantener coherencia con el código real.
3. `ALTER TABLE` debe formar parte de la migración; el `.sq` debe representar el esquema final para una instalación nueva. La numeración de `.sqm` determina las transiciones de versión de SQLDelight.
4. Dos campos de intento/error no bastan por sí solos para distinguir una fila permanentemente inválida de una fila que agotó reintentos. El diseño debe hacer esa distinción explícita y comprobable.
5. Un `delay` dentro de un recorrido no define por sí solo cuándo se volverá a intentar esa misma fila. Hay que fijar disparador, persistencia de espera y conducta tras reiniciar la app.
6. 401 requiere resolver la sesión antes de volver a enviar. 403 requiere revisar autorización. No deben provocar un bucle automático ni pérdida del registro local.
7. Una descarga fallida o incompleta no puede borrar el catálogo anterior ni actualizar la fecha de sincronización completa.
8. La conectividad del dispositivo y la disponibilidad del backend son distintas. El texto de UI debe reflejar evidencia disponible y no inferir modo avión a partir de cualquier error.
9. El puente de permisos del ejemplo necesita atender cancelación, peticiones concurrentes, recreación de Activity y ausencia de Activity. No guardar una referencia caducada indefinidamente.
10. La elección del permiso determina la funcionalidad real que se implementará. Declarar cámara, ubicación y notificaciones indiscriminadamente no acredita una necesidad del producto.
11. El ejemplo de `TelemetriaFirebase` tiene métodos vacíos: implementar esos métodos y verificar recepción real forma parte del trabajo.
12. La guía muestra Crashlytics y Messaging, pero el resultado verificable habla de un módulo Firebase. Debe cerrarse si se pide telemetría solamente o también notificaciones push reales.
13. Crashlytics, eventos analíticos y push son capacidades distintas: el plan debe especificar qué hará `registrarEvento` y no afirmar que un log equivale a un evento analítico.
14. La compilación y prueba final de iOS requiere un entorno Apple. No se puede certificar desde esta revisión en Windows.
15. La configuración del proveedor y las versiones compatibles deben comprobarse con Gradle y documentación oficial. No asumir compatibilidad solo porque una versión aparece en el PDF.

## 7. Decisión D01: seguridad al reenviar creaciones

Pregunta presentada al usuario: ¿se pueden incluir cambios en el backend PharmaSoft para evitar duplicados cuando se pierde la respuesta de una creación?

Estado: resuelta por el usuario. Respuesta: «Solo móvil; mantener el backend intacto».

Decisión aceptada: alternativa B. El alcance no incluye cambios de contrato REST, persistencia, configuración o código en PharmaSoft. Su inspección y uso como servicio de prueba siguen siendo necesarios.

Consecuencias obligatorias para el diseño:

- Conservar la creación local y su estado cuando no se conozca con certeza el resultado remoto.
- No aplicar reintentos automáticos a un `POST` que pudo haberse ejecutado. Un timeout o un error de transporte no demuestra que el servidor no haya recibido la petición.
- Separar «fallo transitorio» de «es seguro repetir esta operación»: son decisiones distintas.
- Permitir reintentos automáticos limitados y con espera creciente únicamente en operaciones cuya repetición sea segura.
- Ofrecer revisión del catálogo y una acción manual explícita para resultados inciertos, explicando que puede existir ya un registro en el servidor. No prometer ausencia de duplicados con este contrato.
- Reflejar esta limitación y su justificación en las evidencias y la matriz de cumplimiento. La guía contempla la decisión manual cuando el servicio no admite una clave de operación.

### Alternativa A: aplicación y backend con clave de operación

- Asignar una clave estable a cada creación local antes de su primer envío.
- Conservarla durante todos los reintentos de la misma operación.
- Hacer que PharmaSoft devuelva el resultado de la creación original al recibir la misma clave y contenido.
- Resolver atomicidad y concurrencia en servidor; una comprobación solo en memoria o por nombre no ofrece esa garantía.
- Comprobar pérdida de respuesta, reinicio de la app y dos envíos simultáneos.

Ventaja: permite reintentos automáticos seguros de creación. Coste: contrato REST, persistencia y pruebas adicionales en el backend.

### Alternativa B: aplicación con backend intacto

- Mantener los datos locales y registrar cuando el resultado de una creación sea incierto.
- No reenviar automáticamente una creación que pudo haberse aplicado.
- Ofrecer revisión y una acción manual con explicación de la posible duplicación.
- Distinguir un fallo de lectura reintentable de una creación con efecto remoto incierto.

Ventaja: no cambia el backend. Límite: no puede garantizarse una creación automática sin duplicados tras perder una respuesta; debe quedar documentado y visible.

Comparación histórica de alternativas: A ofrece una garantía de idempotencia que requiere cambiar el servidor; B es la elegida por el usuario y condiciona el resto del plan. No se añadirá A a las tareas de implementación.

## 7.1 Decisión D02: plataformas y evidencias

Estado: resuelta por el usuario. Respuesta: «Validar Android ahora y dejar iOS preparado, con verificación pendiente».

Pregunta presentada: disponibilidad de Mac con Xcode para compilar, ejecutar pruebas y recoger evidencias iOS en la entrega de sesión 13.

Alcance aceptado:

- Android: compilar, ejecutar pruebas pertinentes y recoger evidencias reales de los flujos incluidos en sesión 13.
- iOS: preparar contratos, implementación y configuración que correspondan a las capacidades elegidas; documentar los pasos de compilación, prueba y evidencia que deberán ejecutarse en Mac.
- Informar expresamente de que la compilación, las pruebas, los diálogos nativos y la integración del proveedor en iOS quedan sin verificar hasta disponer de ese entorno.
- No incluir builds iOS firmados ni capturas iOS como entregables ya obtenidos.

No se considerará que una implementación común compiló o funcionó en iOS sin ejecutar las comprobaciones correspondientes en un entorno Apple.

## 7.2 Decisión D03: capacidad que justifica el permiso nativo

Estado: resuelta por el usuario. Respuesta: «Notificaciones locales al terminar la sincronización o detectar filas fallidas (recomendado)».

Pregunta presentada: elegir entre notificaciones locales del resultado de sincronización, cámara para una foto local de producto o notificaciones push con Firebase Cloud Messaging.

Elección aceptada: notificaciones locales de sincronización, por su relación con pendientes y filas fallidas. No se incluyen notificaciones push, obtención de tokens FCM ni envíos desde Firebase Console.

Consecuencias que deberá resolver el diseño:

- Android 13 o superior: solicitar `POST_NOTIFICATIONS` de forma contextual, distinguir los tres estados y comprobar el canal de notificación.
- Versiones anteriores: no mostrar un diálogo de permiso inexistente; comprobar también la disponibilidad real de las notificaciones en los ajustes de la aplicación.
- Mantener el estado y las acciones de sincronización accesibles dentro de la app cuando el usuario deniegue las notificaciones.
- Una notificación local no implica Firebase Messaging ni ejecución de sincronización con la app cerrada. El alcance de ambos se debe declarar por separado.
- Preparar la implementación equivalente de iOS y documentar su verificación pendiente según D02.

Propuesta técnica comunicada: integrar Firebase Crashlytics como proveedor de telemetría detrás de una interfaz de dominio. No sustituye a JWT ni al backend. El detalle de las dependencias y la configuración queda para el diseño; no se considera que Firebase ya esté configurado.

## 7.3 Decisión D04: acceso offline e identidad

Estado: resuelta por el usuario. Respuesta: «Solo usuarios previamente autenticados, con datos separados por cuenta (recomendado)».

Hallazgo que origina la decisión: `SesionViewModel.accederModoOffline()` construye una sesión ficticia con rol administrador, y la tabla local no registra al propietario de cada pendiente.

Pregunta presentada: permitir acceso offline solo a usuarios previamente autenticados y separar los datos por cuenta; mantener además una demostración aislada sin envíos al servidor; o exigir red para iniciar/recuperar sesión y permitir offline únicamente una vez dentro.

Elección aceptada: primer acceso autenticado con el backend, identidad local previamente validada para trabajar sin red, pendientes privados por cuenta y autenticación válida antes de enviarlos. No asignar privilegios de administrador por ausencia de conexión. No incluir una demostración anónima con rol administrador.

El diseño debe cubrir la persistencia segura del perfil, el tratamiento de sesión expirada, la recuperación de borradores, el cierre de sesión, la migración de pendientes sin propietario y los escenarios de cambio de cuenta. Mantener datos privados por cuenta implica separar también los borradores, consultas observables, metadatos de sincronización y cualquier tarea en curso, no solo filtrar la lista de productos.

## 7.4 Decisión D05: integración de clientes y pedidos

Estado: resuelta por remisión del usuario a lo que exige la guía. Respuesta: «depende de lo que solicite el documento».

Hallazgo que origina la decisión: las pantallas registran datos principalmente mediante `InMemoryRepository`; los pedidos usan productos de ejemplo, separados del catálogo real persistido.

Pregunta presentada: limitar sesión 13 a productos, sincronización, permisos y Firebase, dejando la integración de clientes/pedidos planificada para sesión 14; integrar clientes ahora; o integrar ambos ahora consumiendo el backend existente.

Aplicación al documento: las páginas 3 y 4 exigen auditar toda la aplicación y decidir qué se cierra hoy, qué pasa a sesión 14 y qué se descarta con justificación. La página 13 exige entregar sincronización refinada, estado observable, permiso nativo real, Firebase, pruebas y evidencias. No exige reimplementar en esta sesión los módulos de clientes/pedidos mediante REST.

Decisión de alcance derivada: productos es el flujo real de integración de sesión 13; clientes y pedidos se auditan, se identifican como módulos de demostración y su integración REST se deja en un backlog explícito para sesión 14. Sí se corrigen en esta sesión los cruces de datos entre cuentas o afirmaciones engañosas en esos módulos, si los hubiera: posponer REST no autoriza mezclar datos ni presentar pedidos de ejemplo como ventas registradas en PharmaSoft.

## 8. Prerrequisitos operativos por comprobar

Las cinco decisiones de alcance están resueltas. Quedan prerrequisitos operativos que el plan debe comprobar y no dar por hechos:

- Firebase: proyecto disponible, configuración y evidencia de recepción real. Crashlytics quedó incluido al aprobar el diseño; se han excluido las push por la elección D03.
- Disponibilidad de un proyecto Firebase para la configuración y de un entorno Android capaz de producir evidencias de los tres estados de notificaciones.

No se preguntarán versiones, rutas o detalles que puedan resolverse leyendo el proyecto. Las decisiones menores de implementación se propondrán con su motivo en el diseño.

## 9. Estructura prevista de los entregables de planificación

El plan definitivo, posterior a la entrevista y al diseño, deberá incluir:

1. Matriz requisito -> tarea -> archivo -> prueba -> evidencia -> criterio de cierre.
2. Diseño de estados y transiciones de sincronización, autenticación y permisos.
3. Contratos de dominio y cambios de persistencia con estrategia de migración sin pérdida.
4. Política de revisión manual de creaciones inciertas y límites de la garantía sin cambios de backend, según D01-B.
5. Tareas ordenadas con dependencias, archivos exactos y resultados observables.
6. Pruebas de fallos transitorios, permanentes, intentos agotados, respuesta perdida y cancelación.
7. Casos de permisos concedido, denegado, definitivo y retorno desde Ajustes.
8. Configuración y verificación real de Firebase por plataforma incluida.
9. Plan de evidencias, informe de actividad autónoma y distribución de commits por los siete pasos.
10. Condiciones de aceptación, límites conocidos y separación de sesión 13 frente al cierre de unidad.

## 10. Referencias técnicas comprobadas

- [SQLDelight 2.1.0: migraciones](https://sqldelight.github.io/sqldelight/2.1.0/android_sqlite/migrations/): `.sq` como esquema actual, `1.sqm` como migración de versión 1 a 2 y verificación mediante snapshots.
- [Android: solicitud de permisos en ejecución](https://developer.android.com/training/permissions/requesting): solicitud contextual, cancelación y degradación de la funcionalidad cuando se deniega.
- [GitLive Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk): integración multiplataforma; iOS necesita enlazar los SDK nativos necesarios.

Las referencias externas complementan la guía. No se ha instalado ningún SDK ni se ha creado/configurado un proyecto externo durante este análisis.

## 11. Límite de la skill solicitada

Se leyó `C:/Users/USER/.codex/skills/grill-me/SKILL.md`. Su única instrucción operativa es `Call the Skill tool with "grilling".` Se buscó `grilling` en las skills locales y plugins disponibles sin encontrarla, y no hay una herramienta `Skill` expuesta en la sesión. No se afirma que esa dependencia se haya ejecutado. La entrevista continúa con preguntas críticas sobre decisiones reales del proyecto, tal como expresa la intención del usuario.
