# Sesión 14 · Paso 6 — Firma y archivo de iOS (NO ejecutado)

> Documento editable. Este paso **no se ejecutó** y se declara como limitación en el informe.

| Dato | Valor |
|---|---|
| Estado | No ejecutado |
| Motivo | No se dispone de macOS con Xcode ni de un dispositivo iOS |
| Impacto | No hay `.ipa` ni archivo de Xcode, ni prueba en dispositivo iOS. La matriz mínima (2 Android + 1 iOS) queda incompleta en la parte iOS. |
| Lo que sí existe | El proyecto `iosApp/` y el módulo `shared` con los targets `iosArm64` e `iosSimulatorArm64` |
| Qué haría falta para cerrarlo | Mac con Xcode 16+, cuenta de Apple (la gratuita basta para instalar en un dispositivo propio; ad hoc requiere el programa de pago) y un iPhone. Seguir los 5 pasos de la guía: equipo de firma, destino "Any iOS Device", Product → Archive, Distribute, exportar `.ipa`. |

La guía indica: *"No se penaliza la limitación; se penaliza no declararla."*
Esta página y la sección de limitaciones de `01-ficha-integracion-y-alcance.md` son esa declaración.
Añade el motivo en el informe con tus palabras: [[ ]]. Si el docente ofrece una alternativa (por ejemplo, usar un equipo del laboratorio), anótala aquí.
