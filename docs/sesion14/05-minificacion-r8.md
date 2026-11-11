# Sesión 14 · Paso 5 — Comprobación de la minificación (R8)

> Documento editable. Completa los campos `[[ ]]`.

## Resultado

| Prueba | Resultado |
|---|---|
| `assembleRelease` con `isMinifyEnabled = true` | Compila sin clases faltantes |
| Release minificado en emulador Pixel_7 (API 34), firmado con clave de depuración solo para esta prueba | **Se cerraba al abrir** (defecto D-01), corregido |
| Tras la corrección: abre y muestra el login | OK (`evidencias/E01-release-minificado-login.png`) |
| Tras la corrección: login con backend apagado | Error controlado, sin crash (`evidencias/E02-release-minificado-error-red.png`) |
| Serialización contra respuestas reales del backend | **[[Pendiente: probar con PharmaSoft encendido]]** |

## Defecto D-01 — el release minificado se cierra al abrir

| Campo | Contenido |
|---|---|
| Dispositivo y versión | Emulador Pixel_7, Android 14 (API 34) |
| Pasos | 1. `./gradlew :androidApp:assembleRelease` con minificación activa. 2. Instalar el APK. 3. Abrir la app. |
| Esperado | Pantalla de login |
| Obtenido | `FATAL EXCEPTION: main — java.lang.StackOverflowError` dentro de `org.koin.core.scope.Scope` / `SingleInstanceFactory` |
| Severidad | Alta (impide todo el recorrido) |
| Estado | **Corregido** |

**Causa.** En `AppModule` `GuardiaCuenta` se registraba como
`single<GuardiaCuenta> { get<AuthRepository>() as GuardiaCuenta }`.
R8 fusionó las interfaces `AuthRepository` y `GuardiaCuenta` dentro de su única implementación,
`AuthRepositorioRest` (ninguna aparece en `mapping.txt`). Koin identifica las dependencias por clase, así que
ambas claves pasaron a ser la misma y el bloque se pedía a sí mismo en bucle. En debug no ocurre, porque no hay fusión.

**Corrección.** Registrar una sola instancia con sus dos contratos:
`single { AuthRepositorioRest(...) } binds arrayOf(AuthRepository::class, GuardiaCuenta::class)`.
Es independiente de cómo R8 renombre o fusione clases.

## Reglas de R8 añadidas (`androidApp/proguard-rules.pro`)

| Regla | Por qué |
|---|---|
| `-keepattributes SourceFile,LineNumberTable` | Trazas legibles en Crashlytics |
| `-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod` | Metadatos que usa la serialización |
| Companion / `serializer(...)` / `$$serializer` de `pe.edu.upeu.pharmamobile2.**` | Serializadores de los DTO y `PerfilLocal` |
| `-dontwarn` de SLF4J, `io.ktor.util.debug` y `java.lang.management` | Referenciados por Ktor y ausentes en Android |

No se añadió ninguna regla para Koin: el defecto se resolvió en el código y no con una regla amplia.

## Decisión sobre la minificación

La minificación **queda activada** en release. [[Si al final la desactivas, escribe aquí el motivo.]]
