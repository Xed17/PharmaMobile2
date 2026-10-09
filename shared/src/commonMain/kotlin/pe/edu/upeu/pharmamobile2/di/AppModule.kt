package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.remote.createHttpClient
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRepositorioRest
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.ActualizarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.EliminarProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ListarProductosUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.ObtenerProductoUseCase
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.presentation.detalle.DetalleProductoViewModel
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel

val dataModule = module {
    single<HttpClient> {
        val baseUrl = runCatching { get<String>(named("baseUrl")) }.getOrDefault("http://10.0.2.2:8080/")
        createHttpClient(get(), baseUrl)
    }
    single<ProductoApi> { ProductoApi(get()) }
    single<ProductoRepository> { ProductoRepositorioRest(get(), categoriaPorDefecto = 26L) }
}

val domainModule = module {
    factory { ListarProductosUseCase(get()) }
    factory { ObtenerProductoUseCase(get()) }
    factory { RegistrarProductoUseCase(get()) }
    factory { ActualizarProductoUseCase(get()) }
    factory { EliminarProductoUseCase(get()) }
}

val presentationModule = module {
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
