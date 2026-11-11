# Sesión 14 · Paso 2 — Almacén de claves del equipo

> Documento editable. **No escribas contraseñas aquí.** Completa los campos `[[ ]]`.

| Dato | Valor |
|---|---|
| Archivo | `pharmamobil-release.jks` |
| Ubicación actual | `C:\Users\USER\Downloads\` (fuera del repositorio) |
| Formato / algoritmo | JKS · RSA 2048 |
| Alias | `pharmamobil` |
| Titular (DN) | `CN=Eduard Chambilla, OU=Facultad de Ingenieria y Arquitectura, O=Universidad Peruana Union, C=PE` |
| Válido | 11/11/2026 → 29/03/2054 |
| Huella SHA-256 del certificado | `EA:A2:C8:23:70:09:45:B7:79:07:2F:E2:B3:19:8E:D9:71:10:86:DC:A8:CA:B8:21:5A:AB:69:F0:EF:12:C1:DA` |
| Contraseñas | En `keystore.properties` (raíz del proyecto, ignorado por Git). Anótalas también en tu gestor de contraseñas. |

## Qué debes hacer tú (obligatorio)

1. **Mover el `.jks` a un lugar permanente.** `Descargas` se limpia con facilidad. Si lo mueves, actualiza `storeFile` en `keystore.properties`.
2. **Hacer una copia de seguridad fuera de esta PC** (USB, nube privada o la PC de un compañero del equipo). La guía lo exige: si se pierde, no hay recuperación y no se puede actualizar la app.
3. **Guardar las dos contraseñas** (almacén y clave) en un gestor de contraseñas. Están en `keystore.properties`.
4. **Nunca subir** el `.jks` ni `keystore.properties` a Git (`.gitignore` ya lo impide; comprueba con `git status`).
5. Completar: Equipo [[N]]. Si el titular real no es `Eduard Chambilla`, hay que crear otro almacén y regenerar los builds.

## Cómo se creó

```bash
keytool -genkeypair -v -keystore pharmamobil-release.jks -storetype JKS \
  -keyalg RSA -keysize 2048 -validity 10000 -alias pharmamobil \
  -dname "CN=Eduard Chambilla, OU=Facultad de Ingenieria y Arquitectura, O=Universidad Peruana Union, C=PE"
```

Las contraseñas se generaron aleatoriamente (20 caracteres alfanuméricos).
