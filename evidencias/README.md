# Carpeta de Evidencias de Capacidades Nativas

Esta carpeta almacena las 6 capturas de pantalla de evidencia requeridas para la entrega de la práctica de **Capacidades Nativas** (`req.md`).

---

## Índice de Archivos de Evidencia Requeridos

| Archivo | Descripción | Plataforma / Contexto |
|---|---|---|
| `01-error-expect-sin-actual.png` | Captura del error de compilación inicial (`Expected formatearSoles has no actual declaration`) provocado intencionalmente antes de implementar `actual`. | Terminal o IDE |
| `02-listado-android-precio-formateado.png` | Captura del emulador Android mostrando el listado de productos con el precio formateado en Soles (`S/ 8.50`). | Emulador Android |
| `03-listado-ios-precio-formateado.png` | Captura de la aplicación ejecutándose en simulador iOS / Xcode con los precios formateados en Soles. | Simulador iOS / Xcode |
| `04-selector-compartir-android.png` | Captura del diálogo o BottomSheet del sistema de Android (`Intent.ACTION_SEND` vía `createChooser`) al pulsar «Compartir» en el detalle. | Emulador Android |
| `05-hoja-compartir-ios.png` | Captura de la hoja de compartir nativa de iOS (`UIActivityViewController`) al pulsar «Compartir» en el detalle. | Simulador iOS / Xcode |
| `06-arquitectura-sin-imports-plataforma.png` | Captura del código o terminal (`grep_search`) demostrando que `commonMain` tiene 0 importaciones de `android.*` o `platform.UIKit.*`. | Terminal / IDE |

---

## Cómo Probar en el Emulador Android

1. Iniciar el backend **PharmaSoft** (`mvn spring-boot:run` en el puerto 8080).
2. Abrir el emulador Android desde Android Studio o ejecutar:
   ```powershell
   & "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd <NombreDeTuAVD>
   ```
3. Ejecutar la app Android:
   ```powershell
   .\gradlew :androidApp:installDebug
   ```
4. Navegar a la pantalla de productos:
   - Se observarán los precios formateados (`S/`).
   - Al tocar un producto, se abre la pantalla de **Detalle**.
   - Al pulsar el botón **Compartir**, se desplegará el selector nativo del sistema con el texto: `[Nombre] - S/ [Precio] - Stock: [Stock]`.
