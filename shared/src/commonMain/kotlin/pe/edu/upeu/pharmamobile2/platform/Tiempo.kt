package pe.edu.upeu.pharmamobile2.platform

expect fun obtenerTiempoActualMs(): Long

fun formatearAntiguedad(timestampMs: Long?, ahoraMs: Long = obtenerTiempoActualMs()): String {
    if (timestampMs == null || timestampMs <= 0L) {
        return "Nunca sincronizado"
    }
    val diffMs = ahoraMs - timestampMs
    if (diffMs < 0) {
        return "Actualizado hace un momento"
    }
    val diffSegundos = diffMs / 1000
    val diffMinutos = diffSegundos / 60
    val diffHoras = diffMinutos / 60
    val diffDias = diffHoras / 24

    return when {
        diffMinutos < 1 -> "Actualizado hace un momento"
        diffMinutos == 1L -> "Actualizado hace 1 minuto"
        diffMinutos < 60 -> "Actualizado hace $diffMinutos minutos"
        diffHoras == 1L -> "Actualizado hace 1 hora"
        diffHoras < 24 -> "Actualizado hace $diffHoras horas"
        diffDias == 1L -> "Actualizado hace 1 día"
        else -> "Actualizado hace $diffDias días"
    }
}
