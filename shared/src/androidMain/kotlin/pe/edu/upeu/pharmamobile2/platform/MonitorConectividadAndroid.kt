package pe.edu.upeu.pharmamobile2.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pe.edu.upeu.pharmamobile2.domain.port.*

/** Foreground only. A default route permits local PharmaSoft even without public internet. */
class MonitorConectividadAndroid(context: Context) : MonitorConectividad {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val main = Handler(Looper.getMainLooper())
    private val _estado = MutableStateFlow(EstadoConectividad.DESCONOCIDA)
    override val estado = _estado.asStateFlow()
    private var callback: ConnectivityManager.NetworkCallback? = null
    override fun disponible() = callback != null && manager.activeNetwork != null
    override fun iniciar() {
        if (callback != null) return
        val nuevo = object : ConnectivityManager.NetworkCallback() {
            private var actual: Network? = null
            override fun onAvailable(network: Network) { main.post {
                if (callback === this) { actual = network; _estado.value = EstadoConectividad.DISPONIBLE }
            } }
            override fun onLost(network: Network) { main.post {
                if (callback === this && actual == network) {
                    actual = null; _estado.value = EstadoConectividad.NO_DISPONIBLE
                }
            } }
        }
        callback = nuevo
        _estado.value = if (manager.activeNetwork != null) EstadoConectividad.DISPONIBLE else EstadoConectividad.NO_DISPONIBLE
        manager.registerDefaultNetworkCallback(nuevo)
    }
    override fun detener() {
        val anterior = callback ?: return
        callback = null
        manager.unregisterNetworkCallback(anterior)
        _estado.value = EstadoConectividad.DESCONOCIDA
    }
}
