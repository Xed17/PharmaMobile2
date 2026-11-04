package pe.edu.upeu.pharmamobile2.di

import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta

/** Owned by the session lifecycle; account and generation are immutable in each scope. */
class ScopesCuenta(private val koin: Koin) {
    private val abiertos = mutableMapOf<ContextoCuenta, Scope>()
    fun abrir(contexto: ContextoCuenta): Scope = abiertos.getOrPut(contexto) {
        koin.createScope("cuenta:${contexto.clave}:${contexto.generacion}", named("cuenta")).also {
            it.declare(contexto)
        }
    }
    fun cerrarAhora(contexto: ContextoCuenta) {
        abiertos.remove(contexto)?.let { scope ->
            scope.getOrNull<kotlinx.coroutines.CoroutineScope>()?.coroutineContext?.get(kotlinx.coroutines.Job)?.cancel()
            scope.getOrNull<pe.edu.upeu.pharmamobile2.data.sync.DisparadorSincronizacion>()?.cerrar()
            scope.close()
        }
    }
    suspend fun cerrar(contexto: ContextoCuenta) { cerrarAhora(contexto) }
}
