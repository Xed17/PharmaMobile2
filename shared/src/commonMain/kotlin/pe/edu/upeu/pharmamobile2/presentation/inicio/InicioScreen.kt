package pe.edu.upeu.pharmamobile2.presentation.inicio

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import pe.edu.upeu.pharmamobile2.presentation.sincronizacion.ResumenSincronizacionCard
import pe.edu.upeu.pharmamobile2.presentation.sincronizacion.SincronizacionUiState
import pharmamobile2.shared.generated.resources.Res
import pharmamobile2.shared.generated.resources.compose_multiplatform

@Composable
fun InicioScreen(
    syncState: SincronizacionUiState? = null,
    onSincronizar: (() -> Unit)? = null,
    onRevisarEnvios: (() -> Unit)? = null,
    onReintentarDescarga: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(Res.drawable.compose_multiplatform),
            contentDescription = "Logo PharmaMobil",
            modifier = Modifier.size(100.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bienvenido a PharmaMobil",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Sistema de Gestión Farmacéutica",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        syncState?.let { state ->
            ResumenSincronizacionCard(
                estado = state,
                onSincronizar = { onSincronizar?.invoke() },
                onRevisarEnvios = onRevisarEnvios,
                onReintentarDescarga = onReintentarDescarga,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Módulos Disponibles",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Productos: Catálogo sincronizado, altas offline y revisión de envíos\n" +
                           "• Clientes: Módulo de demostración\n" +
                           "• Pedidos: Módulo de demostración",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
