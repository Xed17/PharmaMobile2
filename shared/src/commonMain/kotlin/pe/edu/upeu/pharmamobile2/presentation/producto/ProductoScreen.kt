package pe.edu.upeu.pharmamobile2.presentation.producto

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
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.presentation.components.ValidatedTextField

@Composable
fun ProductoScreen() {
    var nombre by remember { mutableStateOf("") }
    var precio by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }

    var errorNombre by remember { mutableStateOf<String?>(null) }
    var errorPrecio by remember { mutableStateOf<String?>(null) }
    var errorStock by remember { mutableStateOf<String?>(null) }

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
            text = "Registro de Producto",
            style = MaterialTheme.typography.titleLarge
        )

        ValidatedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                mensaje = ""
                errorNombre = null
            },
            label = "Nombre del producto",
            error = errorNombre,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        ValidatedTextField(
            value = precio,
            onValueChange = {
                precio = it
                mensaje = ""
                errorPrecio = null
            },
            label = "Precio",
            error = errorPrecio,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        ValidatedTextField(
            value = stock,
            onValueChange = {
                stock = it
                mensaje = ""
                errorStock = null
            },
            label = "Stock",
            error = errorStock,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val validacion = ProductoValidator.validar(nombre, precio, stock)

                if (!validacion.esValido) {
                    errorNombre = validacion.errorNombre
                    errorPrecio = validacion.errorPrecio
                    errorStock = validacion.errorStock
                    mensaje = validacion.mensajeGeneral ?: "Error en los datos ingresados"
                    esError = true
                } else {
                    errorNombre = null
                    errorPrecio = null
                    errorStock = null

                    val precioNumero = precio.toDouble()
                    val stockNumero = stock.toInt()

                    try {
                        val producto = Producto(
                            id = 0,
                            nombre = nombre.trim(),
                            precio = precioNumero,
                            stock = stockNumero
                        )
                        mensaje = "Producto registrado correctamente"
                        esError = false
                    } catch (e: IllegalArgumentException) {
                        mensaje = e.message ?: "Error al registrar el producto"
                        esError = true
                    }
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
