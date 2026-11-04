package pe.edu.upeu.pharmamobile2.presentation.sincronizacion

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ResumenSincronizacionCard(
    estado: SincronizacionUiState,
    onSincronizar: () -> Unit,
    onRevisarEnvios: (() -> Unit)? = null,
    onReintentarDescarga: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (estado.fallidas > 0 || estado.revision > 0)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Estado de Sincronización",
                    style = MaterialTheme.typography.titleMedium
                )
                if (estado.enCurso) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pendientes: ${estado.pendientes}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (estado.pendientes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Fallidas: ${estado.fallidas}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (estado.fallidas > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column {
                    Text(
                        text = "En revisión: ${estado.revision}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (estado.revision > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = estado.antiguedad,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSincronizar,
                    enabled = !estado.enCurso,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (estado.enCurso) "Sincronizando..." else "Sincronizar")
                }

                if ((estado.revision > 0 || estado.fallidas > 0) && onRevisarEnvios != null) {
                    OutlinedButton(
                        onClick = onRevisarEnvios,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revisar envíos")
                    }
                }

                if (estado.puedeReintentarDescarga && onReintentarDescarga != null) {
                    OutlinedButton(
                        onClick = onReintentarDescarga
                    ) {
                        Text("Reintentar descarga")
                    }
                }
            }
        }
    }
}
