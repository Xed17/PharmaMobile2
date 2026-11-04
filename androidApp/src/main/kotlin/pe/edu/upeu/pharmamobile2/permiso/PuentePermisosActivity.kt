package pe.edu.upeu.pharmamobile2.permiso

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import pe.edu.upeu.pharmamobile2.data.permiso.PuenteSolicitudPermiso
import pe.edu.upeu.pharmamobile2.data.permiso.ResultadoSolicitudAndroid
import kotlin.coroutines.resume

class PuentePermisosActivity(
    private val activity: ComponentActivity
) : PuenteSolicitudPermiso {

    private var continuation: CancellableContinuation<ResultadoSolicitudAndroid>? = null

    val launcher: ActivityResultLauncher<String> = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        val res = if (concedido) {
            ResultadoSolicitudAndroid.Concedido
        } else {
            ResultadoSolicitudAndroid.Rechazado
        }
        val cont = continuation
        continuation = null
        cont?.resume(res)
    }

    override suspend fun solicitar(permiso: String): ResultadoSolicitudAndroid =
        suspendCancellableCoroutine { cont ->
            continuation = cont
            cont.invokeOnCancellation {
                continuation = null
            }
            try {
                launcher.launch(permiso)
            } catch (e: Exception) {
                continuation = null
                cont.resume(ResultadoSolicitudAndroid.Cancelado)
            }
        }

    override fun debeMostrarJustificacion(permiso: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permiso)
    }

    override fun abrirAjustes() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        activity.startActivity(intent)
    }
}
