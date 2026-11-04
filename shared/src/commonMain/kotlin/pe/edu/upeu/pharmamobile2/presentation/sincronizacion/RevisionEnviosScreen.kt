package pe.edu.upeu.pharmamobile2.presentation.sincronizacion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.upeu.pharmamobile2.domain.model.DecisionEnvio
import pe.edu.upeu.pharmamobile2.domain.model.EstadoEnvio
import pe.edu.upeu.pharmamobile2.domain.model.RegistroEnvio
import pe.edu.upeu.pharmamobile2.domain.repository.ColaProductoRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisionEnviosScreen(
    colaRepository: ColaProductoRepository,
    onRevisar: (Long, DecisionEnvio, Boolean) -> Unit,
    onVolver: () -> Unit
) {
    val envios by colaRepository.observar().collectAsState(initial = emptyList())
    var productoConfirmandoReenvio by remember { mutableStateOf<RegistroEnvio?>(null) }
    var productoConfirmandoDescarte by remember { mutableStateOf<RegistroEnvio?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Revisión de Envíos") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        if (envios.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay envíos pendientes ni en revisión")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(envios, key = { it.producto.id }) { registro ->
                    FilaEnvioCard(
                        registro = registro,
                        onReenviar = { productoConfirmandoReenvio = registro },
                        onDescartar = { productoConfirmandoDescarte = registro },
                        onRehabilitar = { onRevisar(registro.producto.id, DecisionEnvio.REHABILITAR, true) }
                    )
                }
            }
        }
    }

    productoConfirmandoReenvio?.let { registro ->
        AlertDialog(
            onDismissRequest = { productoConfirmandoReenvio = null },
            title = { Text("Confirmar reenvío") },
            text = {
                Text("El servidor podría haber registrado este producto. Reenviarlo puede duplicarlo.\n\n¿Deseas reenviar '${registro.producto.nombre}'?")
            },
            confirmButton = {
                Button(onClick = {
                    onRevisar(registro.producto.id, DecisionEnvio.REENVIAR, true)
                    productoConfirmandoReenvio = null
                }) {
                    Text("Reenviar de todos modos")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productoConfirmandoReenvio = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    productoConfirmandoDescarte?.let { registro ->
        AlertDialog(
            onDismissRequest = { productoConfirmandoDescarte = null },
            title = { Text("Confirmar descarte") },
            text = { Text("¿Deseas descartar localmente el producto '${registro.producto.nombre}'?") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onRevisar(registro.producto.id, DecisionEnvio.DESCARTAR, false)
                        productoConfirmandoDescarte = null
                    }
                ) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { productoConfirmandoDescarte = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun FilaEnvioCard(
    registro: RegistroEnvio,
    onReenviar: () -> Unit,
    onDescartar: () -> Unit,
    onRehabilitar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (registro.estado) {
                EstadoEnvio.REVISION -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                EstadoEnvio.FALLIDA_AGOTADA, EstadoEnvio.FALLIDA_PERMANENTE -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = registro.producto.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Estado: ${registro.estado.name} (Intentos: ${registro.intentos})",
                style = MaterialTheme.typography.bodySmall
            )
            registro.ultimoError?.let { err ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Error: ${err.mensajeSeguro}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDescartar,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Descartar")
                }

                if (registro.estado == EstadoEnvio.FALLIDA_AGOTADA) {
                    Button(onClick = onRehabilitar) {
                        Text("Rehabilitar")
                    }
                } else if (registro.estado == EstadoEnvio.REVISION) {
                    Button(onClick = onReenviar) {
                        Text("Reenviar")
                    }
                }
            }
        }
    }
}
