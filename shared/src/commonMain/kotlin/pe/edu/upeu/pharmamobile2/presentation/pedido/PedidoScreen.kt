package pe.edu.upeu.pharmamobile2.presentation.pedido

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.presentation.components.ValidatedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen() {
    // Selección de cliente y producto
    var clienteSeleccionado by remember { mutableStateOf<Cliente?>(null) }
    var productoSeleccionado by remember { mutableStateOf<Producto?>(null) }
    var expandedClientes by remember { mutableStateOf(false) }
    var expandedProductos by remember { mutableStateOf(false) }

    // Cantidad
    var cantidad by remember { mutableStateOf("") }

    // Control y envío
    var mensaje by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }
    var intentoRegistrar by remember { mutableStateOf(false) }

    // Errores por campo
    var errorCliente by remember { mutableStateOf<String?>(null) }
    var errorProducto by remember { mutableStateOf<String?>(null) }
    var errorCantidad by remember { mutableStateOf<String?>(null) }

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
            text = "Registro de Pedido",
            style = MaterialTheme.typography.titleLarge
        )

        // Selector de Cliente
        ExposedDropdownMenuBox(
            expanded = expandedClientes,
            onExpandedChange = { expandedClientes = it }
        ) {
            OutlinedTextField(
                value = clienteSeleccionado?.nombre ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Cliente") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClientes) },
                isError = intentoRegistrar && errorCliente != null,
                supportingText = if (intentoRegistrar && errorCliente != null) {
                    {
                        Text(
                            text = errorCliente!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedClientes,
                onDismissRequest = { expandedClientes = false }
            ) {
                if (InMemoryRepository.clientes.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay clientes registrados") },
                        onClick = { expandedClientes = false }
                    )
                } else {
                    InMemoryRepository.clientes.forEach { cliente ->
                        DropdownMenuItem(
                            text = { Text("${cliente.nombre} (${cliente.correo})") },
                            onClick = {
                                clienteSeleccionado = cliente
                                expandedClientes = false
                            }
                        )
                    }
                }
            }
        }

        // Selector de Producto
        ExposedDropdownMenuBox(
            expanded = expandedProductos,
            onExpandedChange = { expandedProductos = it }
        ) {
            val productoTexto = productoSeleccionado?.let {
                "${it.nombre} - S/ ${it.precio} (Stock: ${it.stock})"
            } ?: ""

            OutlinedTextField(
                value = productoTexto,
                onValueChange = {},
                readOnly = true,
                label = { Text("Producto") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProductos) },
                isError = intentoRegistrar && errorProducto != null,
                supportingText = if (intentoRegistrar && errorProducto != null) {
                    {
                        Text(
                            text = errorProducto!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expandedProductos,
                onDismissRequest = { expandedProductos = false }
            ) {
                if (InMemoryRepository.productos.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No hay productos disponibles") },
                        onClick = { expandedProductos = false }
                    )
                } else {
                    InMemoryRepository.productos.forEach { prod ->
                        DropdownMenuItem(
                            text = { Text("${prod.nombre} — S/ ${prod.precio} (Stock: ${prod.stock})") },
                            onClick = {
                                productoSeleccionado = prod
                                expandedProductos = false
                            }
                        )
                    }
                }
            }
        }

        // Campo Cantidad
        ValidatedTextField(
            value = cantidad,
            onValueChange = { cantidad = it },
            label = "Cantidad a solicitar",
            error = errorCantidad,
            mostrarError = intentoRegistrar,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Cálculo dinámico de Subtotal
        val cantidadInt = cantidad.toIntOrNull() ?: 0
        val subtotalCalculado = if (productoSeleccionado != null && cantidadInt > 0) {
            productoSeleccionado!!.precio * cantidadInt
        } else {
            0.0
        }

        Text(
            text = "Subtotal: S/ ${if (subtotalCalculado > 0) ((subtotalCalculado * 100).toLong() / 100.0) else "0.00"}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                intentoRegistrar = true

                val validacion = PedidoValidator.validar(
                    cliente = clienteSeleccionado,
                    producto = productoSeleccionado,
                    cantidad = cantidad
                )

                errorCliente = validacion.errorCliente
                errorProducto = validacion.errorProducto
                errorCantidad = validacion.errorCantidad

                if (!validacion.esValido) {
                    mensaje = validacion.mensajeGeneral ?: "Error en los datos ingresados"
                    esError = true
                } else {
                    val cantidadSolicitada = cantidad.toInt()
                    val nuevoPedido = InMemoryRepository.registrarPedido(
                        cliente = clienteSeleccionado!!,
                        producto = productoSeleccionado!!,
                        cantidad = cantidadSolicitada
                    )

                    mensaje = "Pedido #${nuevoPedido.id} registrado con éxito. Total: S/ ${((subtotalCalculado * 100).toLong() / 100.0)}"
                    esError = false

                    // Limpieza del formulario
                    clienteSeleccionado = null
                    productoSeleccionado = null
                    cantidad = ""
                    intentoRegistrar = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar Pedido")
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