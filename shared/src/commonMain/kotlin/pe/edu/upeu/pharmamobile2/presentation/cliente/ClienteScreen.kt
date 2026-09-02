package pe.edu.upeu.pharmamobile2.presentation.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
    }
}