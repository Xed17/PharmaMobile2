package pe.edu.upeu.pharmamobile2.di

import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile2.data.repository.ProductoRepositorioEnMemoria
import pe.edu.upeu.pharmamobile2.domain.repository.ProductoRepository
import pe.edu.upeu.pharmamobile2.domain.usecase.RegistrarProductoUseCase
import pe.edu.upeu.pharmamobile2.presentation.producto.ProductoViewModel

val dataModule = module {
    single<ProductoRepository> { ProductoRepositorioEnMemoria() }
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
