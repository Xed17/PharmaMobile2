package pe.edu.upeu.pharmamobile2.presentation.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobile2.data.InMemoryRepository
import pe.edu.upeu.pharmamobile2.domain.model.Cliente
import pe.edu.upeu.pharmamobile2.presentation.components.ValidatedTextField

@Composable
fun ClientesScreen() {
    // Estados de texto (String crudo de los TextField)
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    // Control y envío
    var mensaje by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }
    var intentoRegistrar by remember { mutableStateOf(false) }

    // Errores por campo
    var errorNombre by remember { mutableStateOf<String?>(null) }
    var errorCorreo by remember { mutableStateOf<String?>(null) }
    var errorTelefono by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "PHARMAMOBIL",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Registro de Cliente",
            style = MaterialTheme.typography.titleLarge
        )

        ValidatedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = "Nombre completo",
            error = errorNombre,
            mostrarError = intentoRegistrar,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        ValidatedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = "Correo electrónico",
            error = errorCorreo,
            mostrarError = intentoRegistrar,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        ValidatedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = "Teléfono (opcional)",
            error = errorTelefono,
            mostrarError = intentoRegistrar,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                intentoRegistrar = true

                val validacion = ClienteValidator.validar(nombre, correo, telefono)

                errorNombre = validacion.errorNombre
                errorCorreo = validacion.errorCorreo
                errorTelefono = validacion.errorTelefono

                if (!validacion.esValido) {
                    mensaje = validacion.mensajeGeneral ?: "Error en los datos ingresados"
                    esError = true
                } else {
                    val nuevoCliente = Cliente(
                        id = 0L,
                        nombre = nombre.trim(),
                        correo = correo.trim(),
                        telefono = if (telefono.isNotBlank()) telefono.trim() else null
                    )

                    InMemoryRepository.agregarCliente(nuevoCliente)

                    mensaje = "Cliente registrado correctamente"
                    esError = false

                    // Limpieza del formulario tras registro exitoso
                    nombre = ""
                    correo = ""
                    telefono = ""
                    intentoRegistrar = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }

        if (mensaje.isNotBlank()) {
            Text(
                text = mensaje,
                color = if (esError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Acceso a Clientes en Servidor (API)",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Prueba de permisos RBAC: admin y vendedor tienen acceso (200 OK); almacén no tiene permiso (403 Forbidden).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val client: io.ktor.client.HttpClient = org.koin.compose.koinInject()
        val authRepo: pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository = org.koin.compose.koinInject()
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        var consultandoServidor by remember { mutableStateOf(false) }
        var mensajeServidor by remember { mutableStateOf("") }
        var esErrorServidor by remember { mutableStateOf(false) }

        androidx.compose.material3.OutlinedButton(
            onClick = {
                scope.launch {
                    consultandoServidor = true
                    try {
                        val respuesta = client.get("api/v1/clientes")
                        mensajeServidor = "Servidor respondió: HTTP ${respuesta.status.value} OK. Acceso concedido."
                        esErrorServidor = false
                    } catch (e: io.ktor.client.plugins.ClientRequestException) {
                        if (e.response.status.value == 403) {
                            authRepo.notificar403("No tienes permiso para consultar clientes (403)")
                            mensajeServidor = "HTTP 403 Forbidden: Sin permisos para consultar clientes."
                            esErrorServidor = true
                        } else if (e.response.status.value == 401) {
                            authRepo.notificar401()
                            mensajeServidor = "HTTP 401 Unauthorized: Sesión expirada."
                            esErrorServidor = true
                        } else {
                            mensajeServidor = "Error HTTP ${e.response.status.value}: ${e.message}"
                            esErrorServidor = true
                        }
                    } catch (e: Exception) {
                        mensajeServidor = "Error de conexión: ${e.message}"
                        esErrorServidor = true
                    } finally {
                        consultandoServidor = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !consultandoServidor
        ) {
            if (consultandoServidor) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Consultando...")
            } else {
                Text("📡 Consultar Clientes en Servidor (Test 403/200)")
            }
        }

        if (mensajeServidor.isNotBlank()) {
            Text(
                text = mensajeServidor,
                color = if (esErrorServidor) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}