package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile2.data.remote.ProductoApi
import pe.edu.upeu.pharmamobile2.data.remote.createHttpClient
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRepositoryImpl
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel

val dataModule = module {
    single<HttpClient> {
        val baseUrl = runCatching { get<String>(named("baseUrl")) }.getOrDefault("http://10.0.2.2:8080/")
        createHttpClient(get(), baseUrl)
    }
    single<ProductoApi> { ProductoApi(get()) }
    single<ProductoRepository> { ProductoRepositoryImpl(get()) }
}

val domainModule = module {
    factory { RegistrarProductoUseCase(get()) }
}

val presentationModule = module {
    viewModel { ProductoViewModel(get(), get()) }
}

fun initKoin(appDeclaration: KoinAppDeclaration = {}) = startKoin {
    appDeclaration()
    modules(dataModule, domainModule, presentationModule, platformModule)
}
