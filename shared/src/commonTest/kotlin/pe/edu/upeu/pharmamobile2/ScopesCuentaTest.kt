package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import pe.edu.upeu.pharmamobile2.di.ScopesCuenta
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import kotlin.test.*

class ScopesCuentaTest {
    @Test fun cerrarCuentaCancelaSusTrabajos() = runTest {
        val app = koinApplication { }
        try {
            val scopes = ScopesCuenta(app.koin)
            val contexto = ContextoCuenta("A", 1)
            val cuenta = scopes.abrir(contexto)
            val job = kotlinx.coroutines.SupervisorJob()
            cuenta.declare(kotlinx.coroutines.CoroutineScope(job))
            scopes.cerrarAhora(contexto)
            assertFalse(job.isActive)
        } finally { app.close() }
    }
    @Test fun cuentaYGeneracionDeterminanElScopeYSalirLoCierra() = runTest {
        val app = koinApplication { }
        try {
            val scopes = ScopesCuenta(app.koin)
            val a = ContextoCuenta("A", 1)
            val scopeA = scopes.abrir(a)
            assertSame(scopeA, scopes.abrir(a))
            assertEquals(a, scopeA.get<ContextoCuenta>())
            val nueva = scopes.abrir(ContextoCuenta("A", 2))
            assertNotEquals(scopeA.id, nueva.id)
            scopes.cerrar(a)
            assertNull(app.koin.getScopeOrNull(scopeA.id))
            assertNotNull(app.koin.getScopeOrNull(nueva.id))
        } finally { app.close() }
    }
}
