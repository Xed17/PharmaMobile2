# Diseño de sesión 13: PharmaMobile2 sobre PharmaSoft

Fecha: 4 de noviembre de 2026.

Estado: aprobado por el usuario mediante «si» después de presentar este documento. Es la especificación de la implementación; el plan por tareas se mantiene por separado. No se ha implementado código funcional ni se han certificado pruebas.

Documento de requisitos y diagnóstico: `docs/sesion13/00-diagnostico-y-requisitos.md`.

Fuente académica: Guía Práctica N.º 13, `C:/Users/USER/Downloads/sesion13_guia_practica_proyecto_integrador_mw9dtffakg.pdf`.

## 1. Propósito

Entregar una mejora verificable del flujo de productos: guardar trabajo sin red, saber qué falta por enviar y qué falló, recuperar el flujo de sesión, utilizar notificaciones locales de forma contextual y registrar fallos técnicos en Firebase sin exponer información personal.

El resultado debe poder explicarse y demostrarse con pruebas, capturas y una matriz vinculada a los 14 criterios de la guía. Un mensaje de éxito local no se presentará como confirmación del servidor.

## 2. Decisiones y su procedencia

### 2.1 Decisiones del usuario

| ID | Decisión | Consecuencia |
| --- | --- | --- |
| D01 | Solo móvil; mantener backend intacto | No se introduce un endpoint ni una clave de idempotencia reconocida por PharmaSoft |
| D02 | Validar Android; preparar iOS con verificación pendiente | Evidencias reales Android; lista separada de comprobaciones iOS por ejecutar en Mac |
| D03 | Notificaciones locales de sincronización y filas fallidas | Se implementa permiso de notificaciones; no se necesitan push ni FCM |
| D04 | Offline solo para usuarios previamente autenticados, con datos separados por cuenta | Se elimina el administrador ficticio y se separan datos, colas y borradores |
| D05 | Alcance de clientes/pedidos según lo que solicite la guía | Auditoría completa ahora; REST de esos módulos en backlog de sesión 14, con límites declarados |

La interpretación de D05 deriva de las páginas 3-4 y 13: se permite priorizar huecos y dejar trabajo para sesión 14 justificadamente; el entregable específico de esta sesión se concentra en sincronización, permisos, Firebase y sus evidencias.

### 2.2 Propuestas técnicas de este diseño

Estas decisiones son propuestas del agente y se someten a aprobación con el documento:

- Firebase Crashlytics, encapsulado detrás de `Telemetria`; no añadir Analytics ni Messaging.
- Mantener los módulos `androidApp`, `shared` e `iosApp`, Ktor, SQLDelight y Koin existentes.
- Mantener los IDs negativos para productos locales y preservar el paquete SQL existente `pe.edu.upeu.pharmamobil.db`.
- Aislar cada fila y metadato con una clave de cuenta, manteniendo una única base local con consultas obligatoriamente filtradas.
- Conservar los datos anteriores sin propietario en cuarentena local, sin adjudicarlos al siguiente usuario que entre.
- Trabajar con sincronización en primer plano; no incorporar WorkManager, servicios persistentes ni sincronización garantizada con la app cerrada.
- Mantener actualización y eliminación remotas como operaciones en línea. Solo la creación existente se almacena en la cola offline.
- Limitar cada cadena de reintentos seguros a cinco intentos; espera creciente de 1, 2, 4 y 8 segundos entre esos cinco intentos, con función limitada a 16 segundos si se reutiliza.

## 3. Objetivos, exclusiones y condiciones de fracaso

### Objetivos

1. Completar los siete pasos de la guía con correspondencia explícita a sus 14 criterios.
2. Evitar pérdida local de pendientes y sustituciones de catálogo con una descarga parcial.
3. Mantener visibles los fallos permanentes, los intentos agotados y los resultados inciertos.
4. Ofrecer una acción adecuada para cada estado, conservando los datos escritos.
5. Separar usuarios también en operaciones que siguen en curso al cambiar de sesión.
6. Pedir un permiso real solo al activar la función de notificaciones.
7. Obtener evidencia de recepción en Crashlytics y de los tres estados del permiso en Android.

### Fuera de esta entrega

- Cambios de código, esquema, endpoints o configuración del backend.
- Garantía de creación exactamente una vez: requiere apoyo del servidor, no disponible en D01.
- Firebase Authentication, Firestore, Analytics, FCM y campañas push.
- Fotos de productos, cámara y ubicación.
- Cola offline de actualizaciones y eliminaciones.
- Integración REST completa de clientes/pedidos y modelado pedido-venta.
- Sincronización con la aplicación cerrada y tareas persistentes del sistema.
- Publicación, builds iOS firmados o validación iOS inexistente.

### Se considera un fracaso aunque la app compile

- Reenviar automáticamente una creación de resultado incierto.
- Borrar pendientes al refrescar o atribuirlos a una cuenta distinta.
- Ocultar una fila fallida detrás de un contador de pendientes igual a cero.
- Indicar «sincronizado» después de una descarga incompleta o con envíos sin resolver.
- Conceder administrador por entrar sin internet.
- Impedir usar el catálogo porque el usuario deniega notificaciones.
- Dar por verificado Firebase usando solo un adaptador vacío o una prueba falsa.
- Presentar capturas Android como evidencia iOS.

## 4. Alternativas consideradas

| Enfoque | Beneficio | Coste o límite | Decisión |
| --- | --- | --- | --- |
| Móvil y servidor con idempotencia | Permite repetir la misma creación sin duplicarla | Requiere cambios y pruebas backend | Excluido por D01 |
| Móvil con cola y revisión de resultados inciertos | Respeta el servidor existente y conserva el trabajo | Algunas creaciones necesitan revisión humana | Elegido por D01 |
| Repetir todos los `POST` transitorios | Menor complejidad aparente | Puede duplicar productos; contradice la advertencia de la guía | Descartado |
| Telemetría Crashlytics y notificaciones locales | Acredita Firebase y permiso nativo con funciones distintas y útiles | Necesita configuración del proyecto Firebase | Propuesto acorde con D03 |
| Push FCM y telemetría | Permite avisos externos | Añade configuración, tokens y escenarios fuera de D03 | Excluido |

## 5. Arquitectura y límites de responsabilidad

Se conserva la arquitectura por capas. El caso de uso de sincronización coordina contratos de dominio; SQLDelight, HTTP, Firebase y APIs nativas quedan en datos o adaptadores de plataforma.

| Pieza | Responsabilidad |
| --- | --- |
| `ProductoViewModel` | Observar catálogo y resumen; emitir acciones; conservar formulario mediante borrador por cuenta |
| `SincronizarProductosUseCase` | Coordinar selección, subida, resultado por fila, descarga y resumen |
| `ProductoRepository` | CRUD y observación del catálogo por cuenta; no ser un cajón para UI o SDK nativos |
| `ColaProductoRepository` | Consultar y actualizar estado de envío, intentos y error de cada fila |
| `ProductoRemotoRepository` | Exponer enviar y listar sin tipos Ktor ni códigos HTTP fuera de datos |
| `SesionRepository` o evolución de `AuthRepository` | Identidad validada, sesión offline, sesión remota y eventos de expiración |
| `ContextoCuenta` | Clave inmutable y generación de sesión capturadas por una ejecución |
| `GestorPermisos` | Consultar y solicitar permisos; abrir Ajustes |
| `NotificadorSincronizacion` | Publicar/cancelar avisos locales mediante la plataforma |
| `Telemetria` | Registrar contexto técnico controlado sin exponer SDK a presentación o dominio |
| `Reloj` y `Espera` | Tiempo y suspensión sustituibles en pruebas |
| `MonitorConectividad` | Señal local del sistema para UI y disparadores; no certificar que PharmaSoft está disponible |

Los nombres nuevos son contratos propuestos. El plan fijará rutas y firmas exactas, evitará duplicar repositorios ya suficientes y migrará los consumidores existentes de manera coherente.

Koin: infraestructura global para motor HTTP y proveedor; dependencias de cuenta/flujo con ciclo de vida explícito. Una ejecución no consultará una variable mutable de «usuario actual» antes de cada escritura: captura la cuenta al empezar y rechaza resultados de una generación anterior.

## 6. Identidad, offline, borradores y cierre de sesión

### 6.1 Clave y modos de sesión

Clave de cuenta: combinación del identificador estable del entorno backend y el `username` canónico devuelto por una autenticación correcta. No normalizar a minúsculas salvo que el contrato existente lo garantice. La contraseña nunca se persiste. Se conserva el contrato del servidor; no se necesita que login añada un ID de usuario.

| Estado | Entrada | Qué permite |
| --- | --- | --- |
| Sin identidad validada | Instalación nueva o cierre de sesión explícito | Login; no entrar a datos privados sin autenticarse |
| Sesión remota válida | Login o recuperación aceptados por PharmaSoft | Catálogo y operaciones según permisos actuales |
| Acceso local conocido | Reinicio sin red con sesión no cerrada y perfil validado previamente | Consultar caché y preparar creaciones autorizadas por permisos locales; sin envíos remotos |
| Reautenticación necesaria | Token expirado/rechazado | Mantener trabajo privado y ofrecer login; bloquear envíos |
| Sesión cerrada | Acción explícita de salir | Cancelar tareas, limpiar token/identidad activa y bloquear reapertura offline hasta nuevo login |

El acceso offline es continuidad local de un usuario conocido, no comprobación de identidad frente al servidor ni conocimiento de revocaciones posteriores. Si la app ya recibió un 401, no vuelve a elevar ese token a válido por perder la red.

Perfil, fecha de autenticación y estado de continuidad local se guardan en almacenamiento seguro. El adaptador Android actual recurre a preferencias sin cifrar cuando falla el cifrado; se eliminará esa degradación silenciosa y se mostrará un error recuperable de almacenamiento. iOS conserva Keychain y comprobará estados de error, sin certificar su ejecución en Windows.

### 6.2 Borrador y expiración

- Persistir por cuenta los campos del formulario de producto sin contraseñas ni token.
- Guardar el borrador al editar, con escritura breve y controlada; no depender solo de desmontar la pantalla.
- Si llega 401, conservar borrador y fila local, cancelar la sincronización y explicar que se necesita iniciar sesión.
- Tras login de la misma cuenta, ofrecer continuar el borrador y revisar los envíos bloqueados.
- Tras login de otra cuenta, mostrar solo su propio estado; no copiar formularios ni pendientes.
- No restaurar un borrador que una confirmación local de guardado ya dio por consumido.

### 6.3 Salida y datos retenidos

Al salir: cancelar y esperar operaciones de la cuenta, invalidar su generación, cerrar observadores, borrar token y caché HTTP de auth, limpiar UI y avisos visibles. Conservar pendientes y borradores en almacenamiento privado, inaccesibles para otra cuenta, hasta que la misma cuenta vuelva a autenticarse.

No borrar silenciosamente trabajo no enviado. La salida debe decir que se conservará de forma local para esa cuenta. Un reinicio después de salir no permite reabrirla offline.

Los módulos de demostración de clientes/pedidos deben vaciar su estado en memoria al salir/cambiar de cuenta; sus registros de ejemplo no se convertirán en envíos reales. Se describen como demostración y se deja su integración en el backlog.

## 7. Persistencia, migración y estados por fila

### 7.1 Esquema objetivo

Mantener `producto`, ampliar sus datos y pasar a clave primaria compuesta `(cuentaClave, id)`. Todas las lecturas, escrituras, borrados y confirmaciones reciben la cuenta. Los IDs remotos pueden repetirse en cachés de distintas cuentas; los IDs negativos se asignan por cuenta dentro de una transacción.

| Campo o grupo | Propósito |
| --- | --- |
| `cuentaClave`, `id` | Separación y compatibilidad con IDs existentes |
| `nombre`, `precio`, `stock` | Datos actuales |
| `activo`, `categoriaId`, `categoriaNombre` | Conservar los datos reales del catálogo y del formulario |
| `pendiente` | Compatibilidad y filtro de conservación; consistente con `estadoEnvio` |
| `estadoEnvio` | Pendiente, enviando, confirmado, revisión, fallida, requiere sesión o permiso |
| `intentos` | Número de intentos de envío efectivamente iniciados; no contar observaciones ni estar sin red |
| `ultimoErrorCodigo`, `ultimoError` | Clasificación técnica y mensaje sanitizado, sin respuesta HTTP cruda |
| `erroresCampos` | Errores de validación necesarios para corregir una creación rechazada |
| `proximoIntentoEn` | Tiempo mínimo de un nuevo intento, persistido si aplica |
| `operacionLocalId` | Identificador estable local para trazabilidad y protección frente a acciones repetidas; no idempotencia del servidor |

Metadatos: claves por cuenta para última descarga completa y estado de intentos seguros de lectura. Borradores: tabla por cuenta y formulario. Preferencia de avisos: por cuenta; permiso del sistema: por instalación.

Los campos exactos, restricciones y representación de errores estructurados serán fijados en el plan. Un estado explícito es obligatorio; no sobrecargar el contador para representar todas las causas de fallo.

### 7.2 Migración sin atribución inventada

- Guardar snapshot verificable del esquema anterior y preparar `1.sqm` si la versión actual sigue siendo 1.
- Reconstruir las tablas que requieren nueva clave primaria; el `.sq` describe una instalación nueva con el esquema final.
- Conservar las filas anteriores bajo `legacy_unassigned`, separadas de cualquier cuenta autenticada.
- Las categorías y estados que el esquema antiguo nunca guardó no se pueden recuperar inventando valores; marcar los datos como incompletos donde corresponda.
- No enviar ni mostrar detalles de filas sin propietario al siguiente usuario. Conservarlas intactas para recuperación asistida posterior, fuera del envío normal de sesión 13.
- Informar de forma general si existen datos anteriores sin atribuir y documentar la recuperación pendiente. No mostrar sus nombres o valores en ese aviso.
- Comprobar que la migración es equivalente al esquema nuevo y que preserva los valores antiguos. No exigir desinstalar para aprobar esta verificación.

No se crean migraciones en PharmaSoft.

### 7.3 Estados y transiciones de envío

| Estado | Significado | Acción permitida |
| --- | --- | --- |
| `Pendiente` | Creación local todavía no intentada o liberada tras rechazo definitivo corregido | Primer envío con sesión válida y cuenta coincidente |
| `Enviando` | Intento marcado en disco antes de iniciar HTTP | Impedir otro envío simultáneo |
| `Confirmado` | Se recibió respuesta válida y se confirmó en transacción | Catálogo normal con ID servidor |
| `Revision` | No se conoce si se aplicó el efecto remoto | Descargar y revisar; reenviar solo con acción explícita y advertencia |
| `FallidaPermanente` | Validación, conflicto u otro rechazo que necesita corrección | Revisar, corregir y confirmar; no repetir automáticamente |
| `FallidaAgotada` | Cinco intentos iniciados sin resolución | Mantener visible, revisar y rehabilitar explícitamente |
| `RequiereSesion` | Rechazo 401 | Reautenticar; revisar antes de liberar la fila |
| `RequierePermiso` | Rechazo 403 | Revisar privilegios; no resolver mediante el permiso de notificaciones |

Un proceso que arranca y encuentra `Enviando` lo convierte en `Revision`; el cierre del proceso no demuestra que el servidor no haya creado el producto.

Una fila en revisión no admite editar su payload para reenviarlo sin una decisión humana que cierre el intento anterior. Una fila rechazada definitivamente sí puede corregirse: preservar el rechazo y autorizar una nueva revisión lógica del envío con contador reiniciado; nunca reiniciar automáticamente el contador para evadir el límite.

## 8. Sincronización y reintentos

### 8.1 Clasificación de resultados

La capa de datos traduce `ErrorApi` a un resultado de dominio. Debe conservar dos dimensiones: naturaleza del fallo y seguridad de repetir el efecto.

| Resultado | Lectura GET | Creación POST sin idempotencia |
| --- | --- | --- |
| Sin red / timeout | Transitorio reintentable con límite | Si se inició HTTP, resultado incierto; revisión manual |
| Error servidor / 503 | Transitorio reintentable con límite | No demuestra ausencia de efecto; revisión manual |
| 400 | Permanente | Fallida con errores de campo; corregir |
| 404 | Permanente | Fallida; revisar recurso/categoría |
| 409 | Permanente | Fallida; revisar conflicto, sin vincular por nombre automáticamente |
| 401 | Detener y resolver sesión | Detener cola y conservar fila privada |
| 403 | Detener operación no autorizada | Conservar fila y explicar privilegios |
| Error de deserialización | Error de datos inesperado, no fingir falta de red | Si hubo POST, revisión; el servidor pudo haberlo aplicado |
| Cancelación | Propagar cancelación | Propagar; conservar estado de intento para revisión |

No contar un ciclo omitido por modo local como intento de producto. No usar `catch(Throwable)` para tratar un fallo de programación como ausencia de internet.

### 8.2 Ciclo de sincronización

1. Capturar cuenta y generación; adquirir exclusión mutua del coordinador.
2. Verificar que existe sesión remota utilizable de esa cuenta. Si no, devolver un resumen bloqueado sin enviar.
3. Seleccionar filas elegibles de esa cuenta. Excluir cuarentena, fallidas, revisión y cualquier intento ya iniciado.
4. Para cada fila, verificar que la sesión sigue vigente; marcar `Enviando` y aumentar intentos en transacción breve.
5. Ejecutar una creación. Confirmar respuesta e ID remoto en transacción o persistir el estado/motivo correspondiente.
6. Un fallo de validación de una fila no bloquea las siguientes. Un 401 detiene envíos y descarga; una pérdida global de conectividad detiene trabajo inútil sin consumir intentos de filas no iniciadas.
7. Después de la fase de subida, obtener todas las páginas del catálogo con orden estable por ID. Las filas en revisión o fallidas permanecen locales.
8. Usar reintentos limitados en lecturas seguras. Esperar fuera de transacciones SQL y sin bloquear el hilo principal.
9. Solo con todas las páginas válidas, conciliar en una transacción las filas confirmadas de esa cuenta y actualizar su timestamp. Si una página falla, conservar íntegro el catálogo anterior.
10. Emitir un resumen que distinga confirmadas, pendientes sin intentar, fallidas, en revisión, bloqueadas por sesión y resultado de descarga. Publicar aviso local cuando corresponda.

La paginación completa evita el límite actual de 20 productos. No puede ofrecerse una instantánea consistente ante cambios concurrentes del servidor si el API no la proporciona; usar orden estable, evitar IDs repetidos y documentar esa limitación sin inventar un contrato nuevo.

### 8.3 Disparadores y esperas

- Abrir el catálogo con sesión remota válida.
- Guardar localmente: devolver éxito local de inmediato y solicitar al coordinador el primer envío, sin esperar el timeout HTTP en el formulario.
- Recuperar conectividad mientras la app esté activa; agrupar eventos y no crear ciclos paralelos.
- Botón de sincronizar: iniciar el ciclo elegible; nunca liberar implícitamente creaciones en revisión.
- Botón de revisión de una fila: acción individual explícita, con sesión válida, espera aplicable y límite de intentos.
- Detener al salir de cuenta o cerrar el proceso; no prometer ejecución en segundo plano.

La unidad de reintento de lectura es una descarga completa del catálogo de una cuenta, no cada página por separado. Una cadena tiene un ID estable y como máximo cinco recorridos completos iniciados. Cada recorrido pide las páginas en orden desde la primera y conserva sus resultados provisionalmente; si falla cualquier página, no publica ni concilia ese recorrido. El siguiente recorrido vuelve a pedir desde la primera página, evitando persistir una mezcla de páginas de distintos intentos. Es una propuesta de implementación adecuada al catálogo actual, sin afirmar consistencia de snapshot que el servidor no ofrece.

Entre recorridos fallidos transitorios se espera 1, 2, 4 y 8 s. El quinto fallo cierra la cadena sin una espera inútil adicional. La función de espera tiene techo de 16 s, comprobable con tiempo virtual. Persistir por cuenta el ID de cadena, intentos iniciados, estado, último error y próximo instante elegible. Reiniciar la app, volver a la pantalla o recibir otro evento de conectividad no restablece esa cadena mientras siga fallida o en espera. Si el proceso termina durante un recorrido, una lectura sigue siendo segura de repetir, pero el intento ya iniciado permanece contabilizado.

Tras cinco fallos, mostrar «No se pudo actualizar el catálogo» con la acción explícita «Reintentar descarga». Solo esa acción crea una cadena nueva de lectura después de un plazo mínimo de 16 s desde el agotamiento; deshabilitarla durante ese plazo y agrupar pulsaciones concurrentes. El botón general de sincronizar y los eventos automáticos no rehabilitan silenciosamente una cadena agotada. Esta acción se refiere únicamente a GET: no libera creaciones en revisión ni restablece sus contadores. Una descarga que termina correctamente cierra su cadena; una sincronización posterior puede crear otra cadena ordinaria. Un 401 pausa el proceso hasta autenticar y un error permanente requiere corregir su causa, sin gastar cuatro intentos automáticos más.

Para creaciones: las esperas no habilitan por sí mismas un reenvío automático. Cinco intentos se aplican a intentos iniciados y autorizados de la misma operación local. «Revisar y habilitar» tras agotamiento es una decisión explícita registrada, no un bucle ilimitado.

### 8.4 Revisión manual sin falsa deduplicación

Mostrar el motivo y descargar el catálogo para comparación. Coincidir por nombre/precio no prueba que un registro sea el resultado de ese intento. No marcar como confirmado automáticamente por semejanza.

Acciones: mantener sin enviar; descartar la fila local con confirmación; o reenviar conscientemente tras advertir que el servidor pudo haberlo registrado. El descarte no borra ningún producto remoto. No se promete evitar el duplicado si el usuario elige reenviar.

### 8.5 Relación con los criterios de la guía

La guía advierte expresamente que solo se reintenta automáticamente lo idempotente o se deja la decisión al usuario. El informe explicará que las lecturas seguras usan backoff automático y que las creaciones inciertas se conservan con revisión manual debido a D01. No marcar como demostrado un reintento automático seguro de `POST` que este backend no puede garantizar.

## 9. Estado de interfaz

Usar consultas observables de la cuenta activa para catálogo, pendientes, fallidas, revisión y última sincronización. Combinar con actividad del coordinador y reloj de actualización de antigüedad. No añadir sondeos HTTP para pintar contadores.

Exponer un resumen visible en Inicio y Productos. La lista debe conservarse durante operaciones y fallos, y permitir abrir los registros que necesitan atención.

| Situación | Mensaje de ejemplo | Acción |
| --- | --- | --- |
| Caché vacía | «Todavía no hay productos guardados en este equipo» | Sincronizar o registrar según sesión/permisos |
| Guardado local | «Guardado en este equipo. Pendiente de envío» | Ver registro |
| Offline conocido | «Sin conexión · 3 cambios por enviar» | Seguir trabajando localmente |
| Fallida | «1 registro necesita corrección» | Revisar errores |
| Resultado incierto | «1 envío necesita revisión» | Abrir explicación y acciones |
| Sesión expirada | «Inicia sesión para enviar tus cambios» | Login sin perder borrador |
| Descarga completa, cola no vacía | «Catálogo actualizado · 2 registros necesitan revisión» | Revisar |
| Nunca sincronizado | «Nunca sincronizado» | Sincronizar |
| Datos antiguos | «Actualizado hace 2 h» | Sincronizar |

Los mensajes son propuestas de contenido, no un rediseño visual completo. Mantener tema, navegación adaptable y componentes existentes. No añadir controles de simulación académica a los flujos normales; las herramientas de prueba existentes deben limitarse a desarrollo.

## 10. Permisos y notificaciones locales

### 10.1 Contratos comunes

`GestorPermisos`: `estado`, `solicitar` y `abrirAjustes`, con `Concedido`, `Denegado`, `DenegadoParaSiempre`. El permiso implementado realmente es notificaciones; no declarar cámara/ubicación sin uso.

`NotificadorSincronizacion`: publicar resumen y cancelar notificaciones de cuenta. El contrato recibe solo cantidades y estado general; no nombres de medicamentos, personas, tokens o precios en la pantalla bloqueada.

### 10.2 Android

- Declarar `POST_NOTIFICATIONS`; crear canal de avisos de sincronización en versiones que lo requieren.
- Solicitar desde una acción «Activar avisos», precedida de explicación y opción de continuar sin ellos.
- API 33+: comprobar concesión, historial de solicitud y rationale. Descartar diálogo/deslizar no se interpreta automáticamente como rechazo definitivo; registrar un resultado real de solicitud.
- API 24-32: no solicitar un runtime permission inexistente; consultar bloqueo de notificaciones de la app y ofrecer Ajustes si corresponde.
- Comprobar además disponibilidad de app/canal: un runtime permission concedido no demuestra que el canal esté habilitado.
- Registrar el puente Activity Result antes del arranque de la Activity; vincular/desvincular el adaptador con su ciclo de vida.
- Una solicitud simultánea espera a la existente o se serializa; no reemplazar un `CompletableDeferred` aún pendiente.
- Al volver de Ajustes o recrear Activity, consultar el estado real. No conservar una referencia a la Activity destruida.
- Denegar no bloquea sincronización ni acceso a la lista.

### 10.3 Política de avisos

Preferencia voluntaria por cuenta. Al terminar un ciclo con envíos confirmados o nuevas filas fallidas/en revisión, emitir un resumen si está habilitada y el sistema lo permite. No emitir por cada refresco vacío ni repetir el mismo fallo sin cambio de estado.

Una notificación por cuenta/ciclo lógico; actualizar o reemplazar para evitar acumulación. Al tocar, abrir el resumen de la cuenta solo si sigue autenticada; en otro caso ir a login. Nunca activar una cuenta desde el enlace de una notificación.

Cancelar avisos de la cuenta al salir. Un error de notificación no cambia el resultado de sincronización. No solicitar permiso al recibir el evento en segundo plano.

### 10.4 iOS preparado

Adaptadores con UserNotifications para consultar autorización, solicitar avisos y publicar notificaciones locales. `notDetermined` se representa como denegado solicitable; `denied` conduce a Ajustes; estados autorizados/provisionales se interpretan según capacidad efectiva y se documentan.

Las notificaciones locales no requieren una descripción de cámara/ubicación ni credenciales APNs de push. No añadir claves de uso ficticias a Info.plist. Configurar solo lo aplicable a los módulos usados y documentar los pasos nativos de Firebase aparte.

Implementación, integración Xcode y comportamiento en primer plano quedan sujetos a comprobación en Mac. D02 impide marcarlos como validados por inspección.

## 11. Firebase y telemetría

### 11.1 Capacidad elegida como propuesta técnica

Crashlytics sirve para fallos no fatales y diagnóstico. `registrarEvento` en este alcance significa breadcrumb/log técnico adjunto al diagnóstico, no un evento analítico de Firebase Analytics.

Contrato de dominio sin imports de GitLive. Adaptador en `data/firebase/TelemetriaFirebase.kt`. Datos y fábricas/DI pueden referenciar la implementación; dominio y presentación solo ven `Telemetria`.

### 11.2 Configuración verificable

- Comprobar primero la línea base actual de Gradle/Kotlin/AGP; no actualizar todo el proyecto como efecto secundario de Firebase.
- La guía propone GitLive 2.7.0: validar que los artefactos y APIs requeridos se resuelven con el proyecto. Si no, documentar la versión compatible y el motivo antes de fijarla.
- Configurar Google Services y Crashlytics en el módulo Android de aplicación; SDK mediante el módulo compartido según documentación compatible.
- Registrar el applicationId existente `pe.edu.upeu.pharmamobile2` en un proyecto Firebase del usuario y disponer de `androidApp/google-services.json` válido.
- No crear una cuenta ni afirmar que existe el proyecto. Disponibilidad, acceso y descarga del archivo son prerrequisitos externos explícitos.
- Verificar la integración nativa de iOS, configuración del bundle, SDK y archivo plist en su proyecto Xcode; documentar los pasos y dejar su ejecución pendiente.
- Pruebas de dominio usan `TelemetriaFalsa`, no Firebase real. Un no-op de prueba no acredita el módulo en el entregable.

### 11.3 Datos permitidos y fallos

Allowlist: pantalla, operación, tipo de error, estado general y cantidades. Nunca username, correo, nombre, contraseña, JWT, cuerpo HTTP ni detalles de producto. Sanitizar también mensajes y causas del `Throwable`; filtrar solo el mapa de contexto no basta.

Una excepción del proveedor no bloquea guardar ni sincronizar. Un error permanente de fila puede registrarse una vez por transición, no por cada recomposición. No tratar cancelaciones ni la ausencia normal de red como un crash crítico repetido.

Prueba de aceptación: emitir un fallo técnico controlado y sanitizado en el build de prueba, comprobar su recepción real en Crashlytics y conservar captura sin identificadores personales. Eliminar o restringir el disparador de prueba al entorno de desarrollo.

## 12. Verificación prevista

Las siguientes son obligaciones del plan, no pruebas ya ejecutadas.

| Área | Comprobaciones mínimas |
| --- | --- |
| Migración | Instalar esquema anterior, migrar sin desinstalar, preservar datos, cuarentena y equivalencia con instalación nueva |
| Cuenta | Misma cuenta restaura solo su trabajo; otra cuenta no observa ni envía esas filas; salida cancela operaciones y bloquea reapertura offline |
| Catálogo | Más de 20 productos, categorías e inactivos; fallo de página intermedia no borra caché ni actualiza timestamp |
| Registro | Guardado local inmediato; ID negativo único bajo concurrencia; solo un primer envío por fila |
| Resultado incierto | POST aplicado con respuesta perdida; ningún segundo POST automático tras red, reinicio o pulsaciones repetidas |
| Fallos permanentes | 400 con campos, 404, 409, 403 y 401; fila visible, flujo recuperable y sin reintento automático inapropiado |
| Backoff | Tiempo virtual 1/2/4/8 s, cinco recorridos máximos por descarga completa, cancelación y presupuesto persistido; recuperación explícita tras agotamiento y plazo de 16 s sin afectar a POST |
| Cola | Una fila inválida no bloquea otra; agotamiento y revisión separados; confirmación y descarga conservan pendientes |
| UI | Observación reactiva, contadores consistentes, antigüedad que cambia con reloj y borrador recuperable |
| Permisos | Primera solicitud, denegación, denegación definitiva, concesión, Ajustes, revocación, recreación y solicitudes concurrentes |
| Notificaciones | Canal/app bloqueados, versión anterior a 33, preferencia por cuenta, no duplicar avisos, tap seguro y cancelación al salir |
| Firebase | Contrato falso en unitarias, aislamiento de imports, sanitización de mensajes y recepción real Android |
| iOS | Lista completa de comandos/pasos por ejecutar; ningún resultado supuesto |

Pruebas compartidas mediante `commonTest` ejecutadas por el target Android host, más pruebas Android para puente y permiso donde corresponda. Los diálogos reales y Ajustes se comprueban en emulador/API 33+ y dispositivo disponible. Se registra claramente si no se dispone de un dispositivo físico.

## 13. Entregables y trazabilidad

1. Tabla del recorrido completo con los ocho escenarios del paso 1, gravedad y decisión.
2. Migración, consultas observables y estados de envío persistidos.
3. Sincronización con resultados por fila, backoff seguro y revisión de creaciones inciertas.
4. Resumen visible y acciones de recuperación.
5. Permiso nativo y notificaciones locales reales Android, con iOS preparado y pendiente.
6. Telemetría Firebase real Android detrás del contrato de dominio.
7. Pruebas, capturas, informe y backlog justificado para sesión 14.

El plan detallado debe asignar cada requisito a archivos, dependencias, pasos, pruebas, evidencia y commit. La guía pide `feature/integrador-13` desde `develop` y un commit por cada uno de sus siete pasos; esa distribución no debe inventarse retrospectivamente después de todos los cambios.

Builds firmados y sustentación se conservan como criterios del cierre de unidad; se documenta su estado sin declararlos entregados en esta etapa de planificación.

## 14. Prerrequisitos y decisiones delegadas al implementador

### Prerrequisitos que deben comprobarse

- Línea base de compilación y pruebas, JDK y SDK Android disponibles.
- Backend accesible y cuentas de prueba autorizadas; el backend permanece sin modificaciones.
- Proyecto Firebase y acceso del usuario para obtener configuración y verificar el informe.
- Emulador API 33+ para evidencias de tres estados; otro API inferior si está disponible para compatibilidad.
- Dispositivo físico si existe, declarando su ausencia si solo se pudo usar emulador.
- Acceso futuro a Mac para cerrar iOS, que no bloquea certificar únicamente la parte Android.

Si falta un prerrequisito externo, el código independiente y la documentación pueden avanzar. La evidencia correspondiente queda pendiente, sin reemplazarla por una simulación presentada como real.

### Decisiones menores delegadas

- Nombres finales de clases y ubicación fina de adaptadores respetando las capas.
- Implementación concreta de reloj, espera, serialización local e IDs de operación.
- Dispatcher y serialización de operaciones de base; sin retener transacciones durante llamadas de red.
- Detalles de componentes Compose reutilizados y accesibilidad de mensajes/acciones.
- Versiones compatibles concretas de plugins y Firebase después de comprobar su resolución.
- Comandos exactos de Gradle obtenidos del proyecto; no inventar nombres de tareas de migración o instrumentación.

Cambiar una decisión D01-D05, introducir reenvío automático incierto, borrar datos sin dueño o añadir un proveedor nuevo sí modifica el diseño y requiere volver a revisarlo.

## 15. Fuentes técnicas

- [Guía Android de permisos](https://developer.android.com/training/permissions/requesting).
- [Permiso de notificaciones Android](https://developer.android.com/develop/ui/compose/notifications/notification-permission).
- [Permisos de notificaciones Apple](https://developer.apple.com/documentation/usernotifications/asking-permission-to-use-notifications).
- [SQLDelight 2.1.0: migraciones multiplataforma](https://sqldelight.github.io/sqldelight/2.1.0/multiplatform_sqlite/migrations/).
- [GitLive Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk).
- [Inicio de Firebase Crashlytics](https://firebase.google.com/docs/crashlytics/get-started).

Las fuentes respaldan las APIs y la configuración; las políticas de cuenta, retención, revisión y alcance son propuestas de este diseño basadas en las decisiones del usuario y en las advertencias de la guía.

## 16. Revisión independiente del diseño

Se realizó una revisión independiente de planificación, sin modificaciones funcionales ni builds. El revisor identificó una decisión pendiente: cuál es la unidad de reintento de descarga y cómo recuperarla después del quinto fallo.

Resolución técnica incorporada en 8.3: cadena por descarga completa y cuenta, cinco recorridos desde la primera página, presupuesto persistido y rehabilitación explícita mediante «Reintentar descarga», con plazo mínimo de 16 s. Se eligió para evitar presupuesto ilimitado por página y para que el usuario no quede bloqueado indefinidamente después del agotamiento. Esta es una propuesta del agente dentro del diseño, no una preferencia atribuida al usuario.

No surgieron preguntas adicionales que exigieran otra decisión de alcance del usuario. El usuario aprobó posteriormente este documento; las propuestas técnicas aquí descritas pasan a ser las decisiones del diseño aprobado.
