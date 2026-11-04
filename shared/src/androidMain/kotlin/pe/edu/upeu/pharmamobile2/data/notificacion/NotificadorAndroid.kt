package pe.edu.upeu.pharmamobile2.data.notificacion

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.model.ResumenSincronizacion
import pe.edu.upeu.pharmamobile2.domain.port.EstadoPermiso
import pe.edu.upeu.pharmamobile2.domain.port.GestorPermisos
import pe.edu.upeu.pharmamobile2.domain.port.NotificadorSincronizacion
import pe.edu.upeu.pharmamobile2.domain.port.Permiso

class NotificadorAndroid(
    private val context: Context,
    private val gestorPermisos: GestorPermisos
) : NotificadorSincronizacion {

    companion object {
        const val CANAL_ID = "sincronizacion"
    }

    init {
        crearCanalSiAplica()
    }

    private fun crearCanalSiAplica() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CANAL_ID,
                "Sincronización",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos del estado de sincronización de productos"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(canal)
        }
    }

    override suspend fun publicar(contexto: ContextoCuenta, resumen: ResumenSincronizacion) {
        val permiso = gestorPermisos.estado(Permiso.NOTIFICACIONES)
        if (permiso !is EstadoPermiso.Concedido) return

        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) return

        val texto = buildString {
            if (resumen.enviadas > 0) {
                append("${resumen.enviadas} enviados. ")
            }
            if (resumen.fallidas > 0) {
                append("${resumen.fallidas} fallidos. ")
            }
            if (resumen.revision > 0) {
                append("${resumen.revision} en revisión.")
            }
            if (isEmpty()) {
                append("Catálogo actualizado.")
            }
        }.trim()

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("cuenta_clave_aviso", contexto.clave)
        }

        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                context,
                contexto.clave.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        val notification = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Sincronización de productos")
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }
            .build()

        try {
            managerCompat.notify(contexto.clave.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permiso revocado en el instante de publicar
        }
    }

    override suspend fun cancelar(contexto: ContextoCuenta) {
        val managerCompat = NotificationManagerCompat.from(context)
        managerCompat.cancel(contexto.clave.hashCode())
    }
}
