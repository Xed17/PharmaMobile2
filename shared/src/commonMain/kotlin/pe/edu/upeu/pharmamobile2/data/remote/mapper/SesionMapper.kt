package pe.edu.upeu.pharmamobile2.data.remote.mapper

import pe.edu.upeu.pharmamobile2.data.remote.dto.LoginResponseDto
import pe.edu.upeu.pharmamobile2.data.remote.dto.UsuarioActualDto
import pe.edu.upeu.pharmamobile2.domain.model.Sesion

fun LoginResponseDto.toDomain(): Sesion = Sesion(
    token = token,
    tipo = tipo,
    expiraEnSegundos = expiraEnSegundos,
    username = username,
    nombreCompleto = nombreCompleto,
    roles = roles,
    permisos = permisos.toSet()
)

fun UsuarioActualDto.toDomain(
    token: String,
    tipo: String = "Bearer",
    expiraEnSegundos: Long = 3600L
): Sesion = Sesion(
    token = token,
    tipo = tipo,
    expiraEnSegundos = expiraEnSegundos,
    username = username,
    nombreCompleto = empleadoNombre ?: username,
    roles = roles,
    permisos = permisos.toSet()
)
