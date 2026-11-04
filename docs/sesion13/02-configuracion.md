# Entorno y requisitos de configuración — Sesión 13

- **Worktree:** `C:/Users/USER/.codex/worktrees/sesion13-integrador/PharmaMobile2`
- **Rama:** `feature/integrador-13`
- **Gradle wrapper:** 9.1.0; daemon JDK 21.
- **SDK Android:** `C:/Users/USER/AppData/Local/Android/Sdk`.
- **AVD:** Pixel_7 (Android API 34).
- **Backend PharmaSoft:** Verificado en ejecución en `http://localhost:8080/api/health` (HTTP 200). En emulador Android mapeado a `http://10.0.2.2:8080/`.
- **Firebase:** Proyecto registrado con éxito por el usuario:
  - **Project ID:** `pharmamobile2`
  - **Project Number:** `472924621717`
  - **Package Name / ApplicationId:** `pe.edu.upeu.pharmamobile2`
  - **Archivo de configuración:** `androidApp/google-services.json` incorporado y aplicado mediante plugin `com.google.gms.google-services`.
  - **Telemetría y Crashlytics:** Módulo `TelemetriaFirebase` integrado detrás de la interfaz de dominio `Telemetria` con envoltura de sanitización obligatoria `TelemetriaSegura` (evita fuga de tokens, emails o datos sensibles).
- **iOS:** Adaptadores preparados en Kotlin Multiplatform (`shared/src/iosMain`); ejecución y validación documentada en `docs/sesion13/06-verificacion-ios-pendiente.md`.
