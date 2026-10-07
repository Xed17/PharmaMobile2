package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile2.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile2.platform.CompartidorIos

actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single(named("baseUrl")) { "http://localhost:8080/" }
    single<Compartidor> { CompartidorIos() }
}
