package pe.edu.upeu.pharmamobile2.data.permiso

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pe.edu.upeu.pharmamobile2.domain.port.EstadoPermiso
import pe.edu.upeu.pharmamobile2.domain.port.GestorPermisos
import pe.edu.upeu.pharmamobile2.domain.port.Permiso

class GestorPermisosAndroid(
    private val context: Context?,
    private val puente: PuenteSolicitudPermiso = PuentePermisosHolder,
    private val sdkInt: Int = Build.VERSION.SDK_INT,
    private val estaConcedidoEnSO: (permiso: String) -> Boolean = { perm ->
        if (context == null) false
        else if (perm == Manifest.permission.POST_NOTIFICATIONS && sdkInt >= 33) {
            ContextCompat.checkSelfPermission(context, perm) == PermissionChecker.PERMISSION_GRANTED
        } else if (perm == Manifest.permission.POST_NOTIFICATIONS && sdkInt < 33) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            ContextCompat.checkSelfPermission(context, perm) == PermissionChecker.PERMISSION_GRANTED
        }
    },
    private val yaSolicitado: () -> Boolean = {
        context?.getSharedPreferences("permisos_prefs", Context.MODE_PRIVATE)
            ?.getBoolean("solicitado_post_notif", false) ?: false
    },
    private val marcarSolicitado: () -> Unit = {
        context?.getSharedPreferences("permisos_prefs", Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("solicitado_post_notif", true)
            ?.apply()
    }
) : GestorPermisos {

    private val mutex = Mutex()
    private var solicitudEnCurso: CompletableDeferred<EstadoPermiso>? = null

    override suspend fun estado(permiso: Permiso): EstadoPermiso {
        return when (permiso) {
            Permiso.NOTIFICACIONES -> {
                if (sdkInt < 33) {
                    if (estaConcedidoEnSO(Manifest.permission.POST_NOTIFICATIONS)) {
                        EstadoPermiso.Concedido
                    } else {
                        EstadoPermiso.DenegadoParaSiempre
                    }
                } else {
                    if (estaConcedidoEnSO(Manifest.permission.POST_NOTIFICATIONS)) {
                        EstadoPermiso.Concedido
                    } else if (yaSolicitado() && !puente.debeMostrarJustificacion(Manifest.permission.POST_NOTIFICATIONS)) {
                        EstadoPermiso.DenegadoParaSiempre
                    } else {
                        EstadoPermiso.Denegado
                    }
                }
            }
        }
    }

    override suspend fun solicitar(permiso: Permiso): EstadoPermiso {
        return when (permiso) {
            Permiso.NOTIFICACIONES -> {
                if (sdkInt < 33) {
                    return if (estaConcedidoEnSO(Manifest.permission.POST_NOTIFICATIONS)) {
                        EstadoPermiso.Concedido
                    } else {
                        abrirAjustes()
                        EstadoPermiso.DenegadoParaSiempre
                    }
                }

                if (estaConcedidoEnSO(Manifest.permission.POST_NOTIFICATIONS)) {
                    return EstadoPermiso.Concedido
                }

                val (miSolicitud, diferido) = mutex.withLock {
                    if (estaConcedidoEnSO(Manifest.permission.POST_NOTIFICATIONS)) {
                        return EstadoPermiso.Concedido
                    }
                    val activo = solicitudEnCurso
                    if (activo != null && !activo.isCompleted) {
                        Pair(false, activo)
                    } else {
                        val nuevo = CompletableDeferred<EstadoPermiso>()
                        solicitudEnCurso = nuevo
                        Pair(true, nuevo)
                    }
                }

                if (!miSolicitud) {
                    return diferido.await()
                }

                try {
                    val resultado = when (puente.solicitar(Manifest.permission.POST_NOTIFICATIONS)) {
                        ResultadoSolicitudAndroid.Concedido -> {
                            marcarSolicitado()
                            EstadoPermiso.Concedido
                        }
                        ResultadoSolicitudAndroid.Cancelado -> {
                            EstadoPermiso.Denegado
                        }
                        ResultadoSolicitudAndroid.Rechazado -> {
                            marcarSolicitado()
                            if (!puente.debeMostrarJustificacion(Manifest.permission.POST_NOTIFICATIONS)) {
                                EstadoPermiso.DenegadoParaSiempre
                            } else {
                                EstadoPermiso.Denegado
                            }
                        }
                    }
                    diferido.complete(resultado)
                    return resultado
                } catch (e: Throwable) {
                    diferido.completeExceptionally(e)
                    throw e
                } finally {
                    mutex.withLock {
                        if (solicitudEnCurso === diferido) {
                            solicitudEnCurso = null
                        }
                    }
                }
            }
        }
    }

    override fun abrirAjustes() {
        puente.abrirAjustes()
    }
}
