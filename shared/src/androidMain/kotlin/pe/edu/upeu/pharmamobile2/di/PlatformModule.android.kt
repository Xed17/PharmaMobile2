package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile2.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile2.platform.CompartidorAndroid

actual val platformModule: Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single(named("baseUrl")) { "http://10.0.2.2:8080/" }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
}
