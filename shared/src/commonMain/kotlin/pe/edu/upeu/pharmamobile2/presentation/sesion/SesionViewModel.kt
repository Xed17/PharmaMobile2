package pe.edu.upeu.pharmamobile2.presentation.sesion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.upeu.pharmamobile2.domain.model.Credenciales
import pe.edu.upeu.pharmamobile2.domain.model.SesionException
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RecuperarSesionUseCase

class SesionViewModel(
    private val authRepository: AuthRepository,
    private val iniciarSesionUseCase: IniciarSesionUseCase,
    private val recuperarSesionUseCase: RecuperarSesionUseCase,
    private val cerrarSesionUseCase: CerrarSesionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SesionUiState())
    val uiState: StateFlow<SesionUiState> = _uiState.asStateFlow()

    private val _snackbarEventos = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarEventos: SharedFlow<String> = _snackbarEventos.asSharedFlow()

    init {
        comprobarSesionInicial()
        escucharEventosSeguridad()
    }

    fun comprobarSesionInicial() {
        viewModelScope.launch {
            _uiState.update { it.copy(estado = EstadoSesion.Comprobando) }
            recuperarSesionUseCase()
                .onSuccess { sesion ->
                    _uiState.update {
                        if (sesion != null) {
                            it.copy(estado = EstadoSesion.Autenticada(sesion))
                        } else {
                            it.copy(estado = EstadoSesion.SinSesion())
                        }
                    }
                }
                .onFailure { error ->
                    val mensaje = error.message ?: "Error al comprobar sesión"
                    _uiState.update {
                        it.copy(estado = EstadoSesion.SinSesion(aviso = mensaje))
                    }
                }
        }
    }

    private fun escucharEventosSeguridad() {
        viewModelScope.launch {
            authRepository.evento401.collect {
                _uiState.update {
                    it.copy(
                        estado = EstadoSesion.SinSesion(
                            aviso = "Su sesión ha expirado. Por favor inicie sesión nuevamente."
                        ),
                        claveInput = "",
                        errorLogin = null
                    )
                }
                _snackbarEventos.tryEmit("Sesión expirada (401). Inicie sesión nuevamente.")
            }
        }

        viewModelScope.launch {
            authRepository.evento403.collect { mensaje ->
                _snackbarEventos.tryEmit(mensaje)
            }
        }
    }

    fun onUsernameChange(valor: String) {
        _uiState.update {
            it.copy(usernameInput = valor, usernameError = null, errorLogin = null)
        }
    }

    fun onClaveChange(valor: String) {
        _uiState.update {
            it.copy(claveInput = valor, claveError = null, errorLogin = null)
        }
    }

    fun toggleClaveVisible() {
        _uiState.update { it.copy(claveVisible = !it.claveVisible) }
    }

    fun seleccionarCuentaRapida(usuario: String, clave: String) {
        _uiState.update {
            it.copy(
                usernameInput = usuario,
                claveInput = clave,
                usernameError = null,
                claveError = null,
                errorLogin = null
            )
        }
    }

    fun iniciarSesion() {
        val actual = _uiState.value
        if (actual.estaCargando) return

        var tieneError = false
        var userErr: String? = null
        var passErr: String? = null

        if (actual.usernameInput.isBlank()) {
            userErr = "Ingrese su usuario"
            tieneError = true
        }
        if (actual.claveInput.isBlank()) {
            passErr = "Ingrese su contraseña"
            tieneError = true
        }

        if (tieneError) {
            _uiState.update {
                it.copy(usernameError = userErr, claveError = passErr)
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(estaCargando = true, errorLogin = null) }
            val credenciales = Credenciales(
                username = actual.usernameInput.trim(),
                clave = actual.claveInput
            )
            iniciarSesionUseCase(credenciales)
                .onSuccess { sesion ->
                    _uiState.update {
                        it.copy(
                            estado = EstadoSesion.Autenticada(sesion),
                            estaCargando = false,
                            claveInput = "",
                            errorLogin = null,
                            usernameError = null,
                            claveError = null
                        )
                    }
                }
                .onFailure { fallo ->
                    val mensaje = (fallo as? SesionException)?.error?.mensaje
                        ?: fallo.message
                        ?: "Error de autenticación"
                    _uiState.update {
                        it.copy(
                            estaCargando = false,
                            errorLogin = mensaje
                        )
                    }
                }
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch {
            cerrarSesionUseCase()
            _uiState.update {
                it.copy(
                    estado = EstadoSesion.SinSesion(),
                    claveInput = "",
                    errorLogin = null
                )
            }
        }
    }
}
