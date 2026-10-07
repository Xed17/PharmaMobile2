package pe.edu.upeu.pharmamobile2.presentation.producto

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobile2.domain.model.Producto
import pe.edu.upeu.pharmamobile2.presentation.components.ValidatedTextField

// Categorías del catálogo backend PharmaSoft
private val CATEGORIAS_DISPONIBLES = listOf(
    26L to "Analgésicos",
    27L to "Antibióticos",
    28L to "Antigripales",
    29L to "Vitaminas",
    30L to "Dermatología"
)

@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel,
    onVerDetalle: (Long) -> Unit = {},
    onCompartir: ((ProductoUi) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()
    val form = state.formulario
    val scrollState = rememberScrollState()

    var tabSeleccionada by remember { mutableStateOf(0) }
    var productoAEliminar by remember { mutableStateOf<ProductoUi?>(null) }

    val estaOperando = state.operacion is ProductoUiState.Operacion.EnCurso

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "PHARMAMOBIL",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (form.estaEnModoEdicion) "Editar Producto #${form.id}" else "Registrar Nuevo Producto",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        // Indicador de operación en curso (Crear / Actualizar / Eliminar)
        if (estaOperando) {
            val tipo = (state.operacion as ProductoUiState.Operacion.EnCurso).tipo
            val textoOp = when (tipo) {
                ProductoUiState.Operacion.Tipo.Crear -> "Registrando producto en PharmaSoft..."
                ProductoUiState.Operacion.Tipo.Actualizar -> "Actualizando producto en PharmaSoft..."
                ProductoUiState.Operacion.Tipo.Eliminar -> "Eliminando producto en PharmaSoft..."
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = textoOp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Mensaje de éxito
        state.mensajeExito?.let { exito ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓ $exito",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = { viewModel.limpiarMensajeExito() }) {
                        Text("Cerrar", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Mensaje de fallo de operación
        if (state.operacion is ProductoUiState.Operacion.Fallida) {
            val msj = (state.operacion as ProductoUiState.Operacion.Fallida).mensaje
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✕ $msj",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.limpiarOperacion() }) {
                        Text("Cerrar", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Campo Nombre con error de validación 400 visible
        ValidatedTextField(
            value = form.nombre,
            onValueChange = { viewModel.actualizarNombre(it) },
            label = "Nombre comercial del producto",
            error = form.nombreError,
            mostrarError = form.nombreError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        // Campo Precio con error de validación 400 visible
        ValidatedTextField(
            value = form.precio,
            onValueChange = { viewModel.actualizarPrecio(it) },
            label = "Precio unitario (S/.)",
            error = form.precioError,
            mostrarError = form.precioError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        // Campo Stock con error de validación 400 visible
        ValidatedTextField(
            value = form.stock,
            onValueChange = { viewModel.actualizarStock(it) },
            label = "Stock disponible en almacén",
            error = form.stockError,
            mostrarError = form.stockError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Selector de Categoría (Evita números mágicos)
        Text(
            text = "Categoría Farmacéutica:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CATEGORIAS_DISPONIBLES.forEach { (catId, catNombre) ->
                FilterChip(
                    selected = form.categoriaId == catId,
                    onClick = { viewModel.actualizarCategoria(catId, catNombre) },
                    label = { Text(catNombre) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Botones de acción del Formulario (Guardar / Actualizar y Cancelar)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.guardar() },
                enabled = !estaOperando,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (form.estaEnModoEdicion) "Actualizar Producto" else "Registrar Producto")
            }

            if (form.estaEnModoEdicion) {
                OutlinedButton(
                    onClick = { viewModel.cancelarEdicion() },
                    enabled = !estaOperando
                ) {
                    Text("Cancelar")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()

        // SECCIÓN INVENTARIO CON TABS Y FASES
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inventario de Productos",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(
                onClick = { viewModel.cargarProductos() },
                enabled = !estaOperando
            ) {
                Text("Recargar")
            }
        }

        when (val fase = state.fase) {
            ProductoUiState.Fase.Cargando -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Consultando catálogo en PharmaSoft...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            ProductoUiState.Fase.SinProductos -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Aún no hay productos registrados en el inventario",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.cargarProductos() }) {
                            Text("Reintentar consulta")
                        }
                    }
                }
            }

            is ProductoUiState.Fase.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = fase.mensaje,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.cargarProductos() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            is ProductoUiState.Fase.ConProductos -> {
                val productosActivos = fase.productos.filter { it.activo && it.stock > 5 }
                val productosInactivos = fase.productos.filter { !it.activo }
                val productosBajoStock = fase.productos.filter { it.activo && it.stock <= 5 }

                val titulosTabs = listOf(
                    "Todos (${fase.productos.size})",
                    "Activos (${productosActivos.size})",
                    "Bajo stock (${productosBajoStock.size})",
                    "Inactivos (${productosInactivos.size})"
                )

                ScrollableTabRow(
                    selectedTabIndex = tabSeleccionada,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    titulosTabs.forEachIndexed { index, titulo ->
                        Tab(
                            selected = tabSeleccionada == index,
                            onClick = { tabSeleccionada = index },
                            text = {
                                Text(
                                    titulo,
                                    fontWeight = if (tabSeleccionada == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                val listaActual = when (tabSeleccionada) {
                    0 -> fase.productos
                    1 -> productosActivos
                    2 -> productosBajoStock
                    else -> productosInactivos
                }

                if (listaActual.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay productos en esta vista",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listaActual.forEach { prod ->
                            ProductoItemCard(
                                producto = prod,
                                estaOperando = estaOperando,
                                onVerDetalle = { onVerDetalle(prod.id) },
                                onCompartir = { onCompartir?.invoke(prod) },
                                onEditar = { viewModel.seleccionarParaEditar(prod) },
                                onEliminar = { productoAEliminar = prod }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Diálogo de confirmación para eliminar
    productoAEliminar?.let { prod ->
        AlertDialog(
            onDismissRequest = { productoAEliminar = null },
            title = { Text("Eliminar Producto") },
            text = { Text("¿Estás seguro de que deseas eliminar '${prod.nombre}'? Esta acción eliminará el registro en PharmaSoft.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = prod.id
                        productoAEliminar = null
                        viewModel.eliminar(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { productoAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ProductoItemCard(
    producto: ProductoUi,
    estaOperando: Boolean,
    onVerDetalle: () -> Unit,
    onCompartir: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Categoría: ${producto.categoriaNombre ?: "General"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Precio: ${producto.precio}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Stock disponible: ${producto.stock} unidades",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val (badgeText, badgeBgColor, badgeTextColor) = when {
                    !producto.activo -> Triple(
                        "Inactivo",
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.onErrorContainer
                    )
                    producto.esBajoStock() -> Triple(
                        "Bajo Stock",
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.onError
                    )
                    else -> Triple(
                        "Activo",
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // Botones de acción: Detalle, Compartir, Editar, Eliminar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onVerDetalle,
                        enabled = !estaOperando
                    ) {
                        Text("Detalle")
                    }
                    Button(
                        onClick = onCompartir,
                        enabled = !estaOperando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Compartir")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onEditar,
                        enabled = !estaOperando
                    ) {
                        Text("Editar")
                    }
                    Button(
                        onClick = onEliminar,
                        enabled = !estaOperando,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}
