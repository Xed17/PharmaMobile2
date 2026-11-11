# Sesión 14 · Paso 4 — Builds firmados y verificación

> Documento editable. Completa los campos `[[ ]]`.

## Artefactos

| Artefacto | Archivo entregado | Uso |
|---|---|---|
| Paquete de aplicación (entregable) | `EquipoN_ProyectoFinal_Android.aab` | Formato de tienda. No se instala en un teléfono. |
| Instalable de revisión | `EquipoN_ProyectoFinal_Android_revision.apk` | Se instala para probar y demostrar. |

Ubicación actual: `C:\Users\USER\Downloads\entregables\`.
**Debes renombrar `EquipoN` con tu número de equipo: [[N]].**

| Dato | Valor |
|---|---|
| `applicationId` | `pe.edu.upeu.pharmamobile2` |
| `versionCode` / `versionName` | `1` / `1.0.0` |
| Backend con el que se compiló | `http://10.40.29.96:8080/` (`local.properties`) |
| SHA-256 del `.apk` | `335d89838be391401c14f610fb3e109bb11bf78a6fddf8937757eb09d706e2ea` |
| SHA-256 del `.aab` | `b8883353942ebfdb1e6e923679a917d505410f474bd11513b9721f38e425cf93` |

> El `.apk` y el `.aab` quedan **atados a la IP del backend** con que se compilaron.
> Si tu IP cambia, actualiza `local.properties` y vuelve a compilar.

## Comandos

```bash
./gradlew :androidApp:bundleRelease     # -> androidApp/build/outputs/bundle/release/androidApp-release.aab
./gradlew :androidApp:assembleRelease   # -> androidApp/build/outputs/apk/release/androidApp-release.apk
apksigner verify --print-certs androidApp/build/outputs/apk/release/androidApp-release.apk
```

## Verificación de la firma

Salida completa en [`04-verificacion-firma.txt`](04-verificacion-firma.txt). Resumen:

| Comprobación | Resultado |
|---|---|
| `apksigner verify` del `.apk` | Verifies (esquema v2) |
| Titular | `CN=Eduard Chambilla, OU=Facultad de Ingenieria y Arquitectura, O=Universidad Peruana Union, C=PE` |
| Huella SHA-256 del certificado | `eaa2c823700945b779072fe2b3198ed9711086dca8cab8215aab69f0ef12c1da` |
| `jarsigner -verify` del `.aab` | jar verified |
| Misma huella en el instalable generado desde el `.aab` | Sí (`bundletool build-apks --mode=universal`) |

> **Captura:** la guía pide una captura de la verificación con titular y huella visibles.
> Ejecuta `apksigner verify --print-certs` en tu terminal y toma la captura: [[adjuntar captura]].

## Prueba del release firmado y minificado (emulador Pixel_7, API 34)

Con el backend real (PharmaSoft en Docker): login con `admin`, perfil, sincronización y catálogo
(**Todos (15)**). Capturas en `evidencias/E03` y `E04`.
Esto valida la serialización y la inyección de dependencias en el build de entrega (ver `05-minificacion-r8.md`).
**No sustituye** la prueba en teléfonos reales (paso 7).
