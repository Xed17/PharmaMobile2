package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.binds
import org.koin.dsl.module
import pe.edu.upeu.pharmamobil.db.PharmaDb
import pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobile2.data.local.ProductoLocal
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.remote.createHttpClient
import pe.edu.upeu.pharmamobile2.data.repository.AuthRepositorioRest
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRemotoRest
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRepositoryOffline
import pe.edu.upeu.pharmamobile2.domain.usecase.ObservarProductosUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ObservarUltimaSincronizacionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.SincronizarProductosUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.CerrarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.IniciarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RecuperarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.sesion.SesionViewModel
import pe.edu.upeu.pharmamobile2.presentation.sincronizacion.SincronizacionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import pe.edu.upeu.pharmamobile2.data.local.BorradorProductoLocal
import pe.edu.upeu.pharmamobile2.data.local.ColaProductoLocal
import pe.edu.upeu.pharmamobile2.data.local.DescargaLocal
import pe.edu.upeu.pharmamobile2.data.platform.EsperaCoroutines
import pe.edu.upeu.pharmamobile2.data.platform.GeneradorUuid
import pe.edu.upeu.pharmamobile2.data.platform.RelojSistema
import pe.edu.upeu.pharmamobile2.data.remote.AutorizadorSolicitudes
import pe.edu.upeu.pharmamobile2.data.sync.CoordinadorSincronizacion
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApi
import pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException
import pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta
import pe.edu.upeu.pharmamobile2.domain.port.Espera
import pe.edu.upeu.pharmamobile2.domain.port.GeneradorIds
import pe.edu.upeu.pharmamobile2.domain.port.GuardiaCuenta
import pe.edu.upeu.pharmamobile2.domain.port.Reloj
import pe.edu.upeu.pharmamobile2.domain.port.SincronizadorProductos
import pe.edu.upeu.pharmamobile2.domain.repository.BorradorProductoRepository
import pe.edu.upeu.pharmamobile2.domain.repository.ColaProductoRepository
import pe.edu.upeu.pharmamobile2.domain.repository.DescargaRepository
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRemotoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.DescargarCatalogoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.EnviarPendientesUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RevisarEnvioUseCase
import pe.edu.upeu.pharmamobile2.platform.InfoDispositivo

import pe.edu.upeu.pharmamobile2.domain.model.CategoriaPredeterminada
import pe.edu.upeu.pharmamobile2.data.remote.ApiConfig
import pe.edu.upeu.pharmamobile2.domain.port.DisparadorSincronizacion
import pe.edu.upeu.pharmamobile2.domain.port.LimpiadorDatosSesion
import pe.edu.upeu.pharmamobile2.data.LimpiadorDatosDemo

val dataModule = module {
    single<Reloj> { RelojSistema() }
    single<Espera> { EsperaCoroutines() }
    single<GeneradorIds> { GeneradorUuid() }
    single<PharmaDb> { PharmaDb(get<CreadorDeDriver>().crear()) }
    single<HttpClient> {
        val baseUrl = runCatching { get<String>(named("baseUrl")) }.getOrDefault(ApiConfig.URL_EMULADOR_ANDROID)
        createHttpClient(
            engine = get(),
            almacenSeguro = get(),
            baseUrl = baseUrl
        )
    }
    // Una sola instancia expuesta con sus dos contratos. Registrarlas por separado con un cast
    // (`get<AuthRepository>() as GuardiaCuenta`) se rompe en release: R8 fusiona las interfaces en
    // AuthRepositorioRest, Koin las ve como la misma clave y la resolucion entra en recursion infinita.
    single {
        AuthRepositorioRest(
            client = get(),
            almacenSeguro = get()
        )
    } binds arrayOf(AuthRepository::class, GuardiaCuenta::class)
    scope(named("cuenta")) {
    scoped { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    scoped { ProductoLocal(get(), get()) }
    scoped { EnviarPendientesUseCase(get(), get(), get(), get(), get()) }
    scoped { DescargarCatalogoUseCase(get(), get(), get(), get(), get(), get(), get()) }
    scoped<SincronizadorProductos> {
        CoordinadorSincronizacion(get(), get(), get(), get(), get(), get(), get(), get(), get(), getOrNull())
    }
    scoped { DisparadorSincronizacion() }
    scoped<ProductoApi> {
        val auth=get<AuthRepository>()
        val contexto=get<ContextoCuenta>()
        ProductoApi(get()) { auth.sesionRemota(contexto)?.token ?: throw ErrorApiException(ErrorApi.NoAutorizado) }
    }
    scoped { AutorizadorSolicitudes(get()) }
    scoped<ProductoRemotoRepository> { ProductoRemotoRest(get(),get(),get()) }
    scoped<ColaProductoRepository> { ColaProductoLocal(get(),get(),get(),get()) }
    scoped<BorradorProductoRepository> { BorradorProductoLocal(get(),get()) }
    scoped<DescargaRepository> { DescargaLocal(get(),get()) }
    scoped<ProductoRepository> {
        ProductoRepositoryOffline(
            local = get(),
            api = get(),
            categoriaPorDefecto = CategoriaPredeterminada.ID,
            authRepository = get(),
            disparador = get()
        )
    }
    scoped { ObservarProductosUseCase(get()) }
    scoped { ObservarUltimaSincronizacionUseCase(get()) }
    scoped { SincronizarProductosUseCase(get()) }
    scoped { RevisarEnvioUseCase(get(),get(),get(),get(),get()) }
    scoped { RegistrarProductoUseCase(get()) }
    scoped { ActualizarProductoUseCase(get()) }
    scoped { EliminarProductoUseCase(get()) }
    viewModel {
        ProductoViewModel(
            observarProductos = get(),
            sincronizarProductos = get(),
            registrarProducto = get(),
            actualizarProducto = get(),
            eliminarProducto = get(),
            observarUltimaSincronizacion = get(),
            borradorRepository = get()
        )
    }
    viewModel {
        DetalleProductoViewModel(
            repository = get(),
            compartidor = get()
        )
    }
    viewModel {
        SincronizacionViewModel(
            cola = get(),
            descarga = get(),
            coordinador = get(),
            revisarEnvio = get(),
            reloj = get(),
            gestorPermisos = getOrNull()
        )
    }
    }
}

val domainModule = module {
    single<LimpiadorDatosSesion> { LimpiadorDatosDemo() }
    factory { IniciarSesionUseCase(get()) }
    factory { RecuperarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get(), get()) }
}
val presentationModule = module {
    single { ScopesCuenta(getKoin()) }
    viewModel { SesionViewModel(get(), get(), get(), get()) }
}

val deviceModule = module {
    single { InfoDispositivo() }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(dataModule, domainModule, presentationModule, deviceModule, platformModule)
}
