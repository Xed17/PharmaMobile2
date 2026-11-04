package pe.edu.upeu.pharmamobile2

import android.os.Bundle
import org.koin.android.ext.android.inject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    private val monitor by inject<pe.edu.upeu.pharmamobile2.domain.port.MonitorConectividad>()
    override fun onStart() { super.onStart(); monitor.iniciar() }
    override fun onStop() { monitor.detener(); super.onStop() }
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}