package pe.edu.upeu.pharmamobile2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.koin.android.ext.android.inject
import pe.edu.upeu.pharmamobile2.data.permiso.PuentePermisosHolder
import pe.edu.upeu.pharmamobile2.permiso.PuentePermisosActivity
import pe.edu.upeu.pharmamobile2.domain.port.MonitorConectividad

class MainActivity : ComponentActivity() {
    private val monitor by inject<MonitorConectividad>()
    private lateinit var puentePermisos: PuentePermisosActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        puentePermisos = PuentePermisosActivity(this)
        PuentePermisosHolder.vincular(puentePermisos)
        super.onCreate(savedInstanceState)

        setContent {
            App()
        }
    }

    override fun onStart() {
        super.onStart()
        monitor.iniciar()
    }

    override fun onStop() {
        monitor.detener()
        super.onStop()
    }

    override fun onDestroy() {
        PuentePermisosHolder.desvincular()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
