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
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobile2.domain.repository.AuthRepository
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
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
    single<PharmaDb> { PharmaDb(get<CreadorDeDriver>().crear()) }
    single { ProductoLocal(get()) }
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
    single<ProductoApi> { ProductoApi(get()) }
    single<ProductoRepository> {
        ProductoRepositorioRest(
            api = get(),
            categoriaPorDefecto = 26L,
            authRepository = get()
        )
    }
}

val domainModule = module {
    factory { ListarProductosUseCase(get()) }
    factory { ObtenerProductoUseCase(get()) }
    factory { RegistrarProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
    factory { IniciarSesionUseCase(get()) }
    factory { RecuperarSesionUseCase(get()) }
    factory { CerrarSesionUseCase(get()) }
}

val presentationModule = module {
    viewModel {
        SesionViewModel(
            authRepository = get(),
            iniciarSesionUseCase = get(),
            recuperarSesionUseCase = get(),
            cerrarSesionUseCase = get()
        )
    }
    viewModel {
        ProductoViewModel(
            listarProductos = get(),
            obtenerProducto = get(),
            registrarProducto = get(),
            actualizarProducto = get(),
            eliminarProducto = get()
        )
    }
    viewModel {
        DetalleProductoViewModel(
            repository = get(),
            compartidor = get()
        )
    }
}

val deviceModule = module {
    single { pe.edu.upeu.pharmamobile2.platform.InfoDispositivo() }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(dataModule, domainModule, presentationModule, deviceModule, platformModule)
}
