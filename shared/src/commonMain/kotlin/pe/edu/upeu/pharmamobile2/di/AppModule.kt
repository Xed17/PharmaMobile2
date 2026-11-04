package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
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
import pe.edu.upeu.pharmamobile2.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RecuperarSesionUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.sesion.SesionViewModel

val dataModule = module {
    single<pe.edu.upeu.pharmamobile2.domain.port.Reloj> { pe.edu.upeu.pharmamobile2.data.platform.RelojSistema() }
    single<pe.edu.upeu.pharmamobile2.domain.port.Espera> { pe.edu.upeu.pharmamobile2.data.platform.EsperaCoroutines() }
    single<pe.edu.upeu.pharmamobile2.domain.port.GeneradorIds> { pe.edu.upeu.pharmamobile2.data.platform.GeneradorUuid() }
    single<PharmaDb> { PharmaDb(get<CreadorDeDriver>().crear()) }
    single<HttpClient> {
        val baseUrl = runCatching { get<String>(named("baseUrl")) }.getOrDefault("http://10.0.2.2:8080/")
        createHttpClient(
            engine = get(),
            almacenSeguro = get(),
            baseUrl = baseUrl
        )
    }
    single<AuthRepository> {
        AuthRepositorioRest(
            client = get(),
            almacenSeguro = get()
        )
    }
    single<pe.edu.upeu.pharmamobile2.domain.port.GuardiaCuenta> { get<AuthRepository>() as pe.edu.upeu.pharmamobile2.domain.port.GuardiaCuenta }
    scope(named("cuenta")) {
    scoped { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main.immediate) }
    scoped { ProductoLocal(get(), get()) }
    scoped { pe.edu.upeu.pharmamobile2.domain.usecase.EnviarPendientesUseCase(get(), get(), get(), get(), get()) }
    scoped { pe.edu.upeu.pharmamobile2.domain.usecase.DescargarCatalogoUseCase(get(), get(), get(), get(), get(), get(), get()) }
    scoped<pe.edu.upeu.pharmamobile2.domain.port.SincronizadorProductos> {
        pe.edu.upeu.pharmamobile2.data.sync.CoordinadorSincronizacion(get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    scoped { pe.edu.upeu.pharmamobile2.data.sync.DisparadorSincronizacion() }
    scoped<ProductoApi> {
        val auth=get<AuthRepository>()
        val contexto=get<pe.edu.upeu.pharmamobile2.domain.model.ContextoCuenta>()
        ProductoApi(get()) { auth.sesionRemota(contexto)?.token ?: throw pe.edu.upeu.pharmamobile2.domain.error.ErrorApiException(pe.edu.upeu.pharmamobile2.domain.error.ErrorApi.NoAutorizado) }
    }
    scoped { pe.edu.upeu.pharmamobile2.data.remote.AutorizadorSolicitudes(get()) }
    scoped<pe.edu.upeu.pharmamobile2.domain.repository.ProductoRemotoRepository> { ProductoRemotoRest(get(),get(),get()) }
    scoped<pe.edu.upeu.pharmamobile2.domain.repository.ColaProductoRepository> { pe.edu.upeu.pharmamobile2.data.local.ColaProductoLocal(get(),get(),get(),get()) }
    scoped<pe.edu.upeu.pharmamobile2.domain.repository.BorradorProductoRepository> { pe.edu.upeu.pharmamobile2.data.local.BorradorProductoLocal(get(),get()) }
    scoped<pe.edu.upeu.pharmamobile2.domain.repository.DescargaRepository> { pe.edu.upeu.pharmamobile2.data.local.DescargaLocal(get(),get()) }
    scoped<ProductoRepository> {
        ProductoRepositoryOffline(
            local = get(),
            api = get(),
            categoriaPorDefecto = 26L,
            authRepository = get(),
            disparador = get()
        )
    }
    scoped { ObservarProductosUseCase(get()) }
    scoped { ObservarUltimaSincronizacionUseCase(get()) }
    scoped { SincronizarProductosUseCase(get()) }
    scoped { pe.edu.upeu.pharmamobile2.domain.usecase.RevisarEnvioUseCase(get(),get(),get(),get(),get()) }
    scoped { ListarProductosUseCase(get()) }
    scoped { ObtenerProductoUseCase(get()) }
    scoped { RegistrarProductoUseCase(get()) }
    scoped { ActualizarProductoUseCase(get()) }
    scoped { EliminarProductoUseCase(get()) }
    viewModel {
        ProductoViewModel(
            observarProductos = get(),
            sincronizarProductos = get(),
            listarProductos = get(),
            obtenerProducto = get(),
            registrarProducto = get(),
            actualizarProducto = get(),
            eliminarProducto = get(),
            observarUltimaSincronizacion = get(),
            productoRepository = get()
        )
    }
    viewModel {
        DetalleProductoViewModel(
            repository = get(),
            compartidor = get()
        )
    }
    }
}

val domainModule = module {
    factory { IniciarSesionUseCase(get()) }
    factory { RecuperarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
}
val presentationModule = module {
    single { ScopesCuenta(getKoin()) }
    viewModel { SesionViewModel(get(), get(), get(), get()) }
}

val deviceModule = module {
    single { pe.edu.upeu.pharmamobile2.platform.InfoDispositivo() }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(dataModule, domainModule, presentationModule, deviceModule, platformModule)
}
