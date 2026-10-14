package pe.edu.upeu.pharmamobile2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.edu.upeu.pharmamobile2.domain.model.Credenciales
import pe.edu.upeu.pharmamobile2.domain.model.ErrorSesion
import pe.edu.upeu.pharmamobile2.domain.model.Sesion
import pe.edu.upeu.pharmamobile2.domain.model.SesionException
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RecuperarSesionUseCase
import pe.edu.upeu.pharmamobile2.presentation.sesion.EstadoSesion
import pe.edu.upeu.pharmamobile2.presentation.sesion.SesionViewModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeAuthRepository(
    var sesionRetornada: Sesion? = null,
    var errorALoguear: Throwable? = null,
    var errorAlRecuperar: Throwable? = null
) : AuthRepository {

    private val _sesionActual = MutableStateFlow<Sesion?>(sesionRetornada)
    override val sesionActual: StateFlow<Sesion?> = _sesionActual.asStateFlow()

    private val _evento401 = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val evento401: SharedFlow<Unit> = _evento401.asSharedFlow()

    private val _evento403 = MutableSharedFlow<String>(extraBufferCapacity = 1)
    override val evento403: SharedFlow<String> = _evento403.asSharedFlow()

    override suspend fun iniciarSesion(credenciales: Credenciales): Result<Sesion> {
        errorALoguear?.let { return Result.failure(it) }
        val sesion = sesionRetornada ?: Sesion(
            token = "fake.jwt.token",
            username = credenciales.username,
            nombreCompleto = "Usuario ${credenciales.username}",
            roles = listOf("ROLE_ADMIN"),
            permisos = setOf("CATALOGO_LEER", "CATALOGO_ESCRIBIR")
        )
        _sesionActual.value = sesion
        return Result.success(sesion)
    }

    override suspend fun recuperarSesion(): Result<Sesion?> {
        errorAlRecuperar?.let { return Result.failure(it) }
        _sesionActual.value = sesionRetornada
        return Result.success(sesionRetornada)
    }

    override suspend fun cerrarSesion() {
        _sesionActual.value = null
    }

    override fun notificar401() {
        _sesionActual.value = null
        _evento401.tryEmit(Unit)
    }

    override fun notificar403(mensaje: String) {
        _evento403.tryEmit(mensaje)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SesionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel(fakeRepo: FakeAuthRepository): SesionViewModel {
        return SesionViewModel(
            authRepository = fakeRepo,
            iniciarSesionUseCase = IniciarSesionUseCase(fakeRepo),
            recuperarSesionUseCase = RecuperarSesionUseCase(fakeRepo),
            cerrarSesionUseCase = CerrarSesionUseCase(fakeRepo)
        )
    }

    @Test
    fun testArranque_sinSesionPrevia_transicionaASinSesion() = runTest {
        val fakeRepo = FakeAuthRepository(sesionRetornada = null)
        val vm = crearViewModel(fakeRepo)

        advanceUntilIdle()

        assertTrue(vm.uiState.value.estado is EstadoSesion.SinSesion)
    }

    @Test
    fun testArranque_conSesionValida_transicionaAAutenticada() = runTest {
        val sesionExistente = Sesion(
            token = "jwt.stored.token",
            username = "admin",
            nombreCompleto = "Administrador",
            roles = listOf("ADMIN"),
            permisos = setOf("CATALOGO_LEER")
        )
        val fakeRepo = FakeAuthRepository(sesionRetornada = sesionExistente)
        val vm = crearViewModel(fakeRepo)

        advanceUntilIdle()

        val estado = vm.uiState.value.estado
        assertTrue(estado is EstadoSesion.Autenticada)
        assertEquals("admin", estado.sesion.username)
    }

    @Test
    fun testValidacionCamposVacios_muestraErrores() = runTest {
        val fakeRepo = FakeAuthRepository()
        val vm = crearViewModel(fakeRepo)
        advanceUntilIdle()

        vm.iniciarSesion()

        assertNotNull(vm.uiState.value.usernameError)
        assertNotNull(vm.uiState.value.claveError)
        assertFalse(vm.uiState.value.estaCargando)
    }

    @Test
    fun testSeleccionarCuentaRapida_actualizaCampos() = runTest {
        val fakeRepo = FakeAuthRepository()
        val vm = crearViewModel(fakeRepo)
        advanceUntilIdle()

        vm.seleccionarCuentaRapida("vendedor", "Vendedor123")

        assertEquals("vendedor", vm.uiState.value.usernameInput)
        assertEquals("Vendedor123", vm.uiState.value.claveInput)
        assertNull(vm.uiState.value.usernameError)
        assertNull(vm.uiState.value.claveError)
    }

    @Test
    fun testIniciarSesion_exitoso_pasaAAutenticadaYBorraClave() = runTest {
        val fakeRepo = FakeAuthRepository()
        val vm = crearViewModel(fakeRepo)
        advanceUntilIdle()

        vm.onUsernameChange("admin")
        vm.onClaveChange("Admin123")
        vm.iniciarSesion()

        advanceUntilIdle()

        val estado = vm.uiState.value.estado
        assertTrue(estado is EstadoSesion.Autenticada)
        assertEquals("admin", estado.sesion.username)
        assertEquals("", vm.uiState.value.claveInput)
        assertNull(vm.uiState.value.errorLogin)
    }

    @Test
    fun testIniciarSesion_credencialesIncorrectas_muestraError() = runTest {
        val fakeRepo = FakeAuthRepository(
            errorALoguear = SesionException(ErrorSesion.CredencialesIncorrectas())
        )
        val vm = crearViewModel(fakeRepo)
        advanceUntilIdle()

        vm.onUsernameChange("admin")
        vm.onClaveChange("claveInvalida")
        vm.iniciarSesion()

        advanceUntilIdle()

        assertTrue(vm.uiState.value.estado is EstadoSesion.SinSesion)
        assertEquals("Usuario o contraseña incorrectos", vm.uiState.value.errorLogin)
        assertFalse(vm.uiState.value.estaCargando)
    }

    @Test
    fun testCerrarSesion_vuelveASinSesion() = runTest {
        val fakeRepo = FakeAuthRepository()
        val vm = crearViewModel(fakeRepo)
        advanceUntilIdle()

        vm.onUsernameChange("admin")
        vm.onClaveChange("Admin123")
        vm.iniciarSesion()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.estado is EstadoSesion.Autenticada)

        vm.cerrarSesion()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.estado is EstadoSesion.SinSesion)
    }

    @Test
    fun testReaccion401_cierraSesionYNotifica() = runTest {
        val sesionInicial = Sesion(
            token = "jwt.token",
            username = "admin",
            nombreCompleto = "Admin",
            roles = listOf("ADMIN")
        )
        val fakeRepo = FakeAuthRepository(sesionRetornada = sesionInicial)
        val vm = crearViewModel(fakeRepo)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.estado is EstadoSesion.Autenticada)

        // Se produce 401 en el backend
        fakeRepo.notificar401()
        advanceUntilIdle()

        val estado = vm.uiState.value.estado
        assertTrue(estado is EstadoSesion.SinSesion)
        assertTrue(estado.aviso?.contains("expirado") == true)
    }

    @Test
    fun testReaccion403_mantieneSesionYEmiteSnackbar() = runTest {
        val sesionInicial = Sesion(
            token = "jwt.token",
            username = "almacen",
            nombreCompleto = "Almacenero",
            roles = listOf("ALMACEN")
        )
        val fakeRepo = FakeAuthRepository(sesionRetornada = sesionInicial)
        val vm = crearViewModel(fakeRepo)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.estado is EstadoSesion.Autenticada)

        // Se produce 403 al intentar consultar clientes
        fakeRepo.notificar403("No tienes permiso para consultar clientes (403)")
        advanceUntilIdle()

        // La sesión se DEBE MANTENER
        assertTrue(vm.uiState.value.estado is EstadoSesion.Autenticada)
    }
}
