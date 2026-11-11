# Sesión 14 · Paso 7 — Matriz de pruebas en dispositivos reales

> **Plantilla para completar tú.** No hay resultados inventados: cada celda se llena con lo que observes
> al ejecutar el recorrido **con el `.apk` firmado**, con el equipo desconectado del entorno de desarrollo.

## Requisitos antes de salir a probar

- PharmaSoft encendido (Docker + backend) y la PC con IP `10.40.29.96` (compruébalo con `ipconfig`; si cambió, recompila).
- Teléfono y PC en la **misma red Wi-Fi**.
- Cortafuegos de Windows permitiendo el puerto **8080** de entrada (ver el final de este documento).
- Prueba rápida en el navegador del teléfono: `http://10.40.29.96:8080/api/v1/auth/login`. Cualquier respuesta (405 o 401) prueba que llega.

## Dispositivos

| # | Marca y modelo | Versión de Android | Gama / antigüedad | Quién lo prestó |
|---|---|---|---|---|
| 1 | [[ ]] | [[ ]] | [[ ]] | [[ ]] |
| 2 | [[ ]] | [[ ]] | [[ ]] | [[ ]] |
| 3 (autónoma) | [[ ]] | [[ ]] | [[ ]] | [[ ]] |
| iOS | — | — | — | No disponible (ver `06-ios-limitacion.md`) |

## Recorrido (marca OK / FALLA / N/A y anota el defecto si falla)

| N.º | Paso | Qué se observa | Disp. 1 | Disp. 2 | Disp. 3 |
|---|---|---|---|---|---|
| 1 | Instalar desde el archivo firmado, sin el entorno conectado | Instala; nombre "PharmaMobile" e ícono definitivos | [[ ]] | [[ ]] | [[ ]] |
| 2 | Login correcto (`admin` / `Admin123`) e incorrecto | El mensaje distingue credencial inválida de falta de conexión | [[ ]] | [[ ]] | [[ ]] |
| 3 | CRUD completo con un rechazo de validación | El mensaje del servidor llega al campo que lo originó | [[ ]] | [[ ]] | [[ ]] |
| 4 | Operación sin privilegio (`vendedor` / `Vendedor123` crea un producto, 403) | Se informa el rechazo y la sesión no se cierra | [[ ]] | [[ ]] | [[ ]] |
| 5 | Modo avión: abrir, leer y crear un registro | Sigue mostrando datos; lo creado queda "pendiente" | [[ ]] | [[ ]] | [[ ]] |
| 6 | Recuperar conexión y sincronizar | Lo pendiente sube, sin duplicados, y la pantalla se actualiza sola | [[ ]] | [[ ]] | [[ ]] |
| 7 | Permiso de notificaciones: conceder, denegar y denegar para siempre | Los 3 estados se resuelven; el tercero ofrece ir a Ajustes | [[ ]] | [[ ]] | [[ ]] |
| 8 | Cerrar sesión y reabrir | El token se borró; el equipo queda limpio para otro usuario | [[ ]] | [[ ]] | [[ ]] |

Cuentas de prueba: `admin/Admin123`, `vendedor/Vendedor123`, `almacen/Almacen123`.

## Registro de defectos

Un defecto por bloque. Estado: **Corregido**, **Pendiente** o **Descartado** (con motivo).

### D-01 — El release minificado se cerraba al abrir

| Campo | Contenido |
|---|---|
| Dispositivo y versión | Emulador Pixel_7, Android 14 (detectado antes de probar en teléfonos) |
| Pasos | Instalar el release con R8 activo y abrir la app |
| Esperado | Pantalla de login |
| Obtenido | `StackOverflowError` en Koin al arrancar |
| Severidad | Alta |
| Estado | **Corregido** (ver `05-minificacion-r8.md`) |

### D-02 — [[título]]

| Campo | Contenido |
|---|---|
| Dispositivo y versión | [[ ]] |
| Pasos | [[1. … 2. … 3. …]] |
| Esperado | [[ ]] |
| Obtenido | [[ ]] (adjunta captura) |
| Severidad | [[Alta / Media / Baja]] |
| Estado | [[Corregido / Pendiente / Descartado con motivo]] |

(Copia este bloque para cada defecto nuevo.)

## Si el teléfono no conecta con el backend

1. Mismo Wi-Fi que la PC. Algunas redes institucionales aíslan los dispositivos entre sí; prueba con el punto de acceso de otro teléfono.
2. Permitir el puerto en el cortafuegos (PowerShell **como administrador**):
   `New-NetFirewallRule -DisplayName "PharmaSoft 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow`
3. Confirmar la IP actual con `ipconfig` (adaptador Wi-Fi). Si cambió: editar `api.baseUrl` en `local.properties` y recompilar.
