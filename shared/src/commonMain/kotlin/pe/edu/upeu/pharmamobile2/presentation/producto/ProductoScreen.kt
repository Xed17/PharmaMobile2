package pe.edu.upeu.pharmamobile2.presentation.producto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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

@Composable
fun ProductoScreen(
    viewModel: ProductoViewModel
) {
    val state by viewModel.uiState.collectAsState()

    // Estado efímero de UI
    var tabSeleccionada by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
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
            value = state.nombre,
            onValueChange = { viewModel.actualizarNombre(it) },
            label = "Nombre del producto",
            error = state.errorNombre,
            mostrarError = state.errorNombre != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )

        ValidatedTextField(
            value = state.precio,
            onValueChange = { viewModel.actualizarPrecio(it) },
            label = "Precio",
            error = state.errorPrecio,
            mostrarError = state.errorPrecio != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        ValidatedTextField(
            value = state.stock,
            onValueChange = { viewModel.actualizarStock(it) },
            label = "Stock",
            error = state.errorStock,
            mostrarError = state.errorStock != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = { viewModel.registrar() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrar")
        }

        state.mensajeFormulario?.let { mensaje ->
            if (mensaje.isNotBlank()) {
                Text(
                    text = mensaje,
                    color = if (state.esErrorFormulario) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()

        // SECCIÓN INVENTARIO CON TABS Y FASES
        Text(
            text = "Clasificación de Inventario",
            style = MaterialTheme.typography.titleLarge
        )

        when (val fase = state.fase) {
            FaseProductos.Cargando -> {
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
                            text = "Cargando inventario...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FaseProductos.SinProductos -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay productos registrados en el inventario",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is FaseProductos.Error -> {
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

            is FaseProductos.ConProductos -> {
                val productosActivos = fase.productos.filter { it.activo && it.stock > 5 }
                val productosInactivos = fase.productos.filter { !it.activo }
                val productosBajoStock = fase.productos.filter { it.activo && it.stock <= 5 }

                val titulosTabs = listOf(
                    "Activos (${productosActivos.size})",
                    "Inactivos (${productosInactivos.size})",
                    "Bajo stock (${productosBajoStock.size})"
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
                    0 -> productosActivos
                    1 -> productosInactivos
                    else -> productosBajoStock
                }

                if (listaActual.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay productos en esta categoría",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listaActual.forEach { prod ->
                            ProductoItemCard(prod, tabSeleccionada)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProductoItemCard(producto: Producto, tab: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Precio: S/ ${((producto.precio * 100).toLong() / 100.0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Stock disponible: ${producto.stock} uds.",
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
    }
}
