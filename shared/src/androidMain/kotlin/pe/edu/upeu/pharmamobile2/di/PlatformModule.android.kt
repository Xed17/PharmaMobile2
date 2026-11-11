package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguroAndroid
import pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriverAndroid
import pe.edu.upeu.pharmamobile2.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile2.platform.CompartidorAndroid
import pe.edu.upeu.pharmamobile2.data.firebase.TelemetriaFirebase
import pe.edu.upeu.pharmamobile2.data.firebase.TelemetriaSegura
import pe.edu.upeu.pharmamobile2.data.notificacion.NotificadorAndroid
import pe.edu.upeu.pharmamobile2.data.permiso.GestorPermisosAndroid
import pe.edu.upeu.pharmamobile2.domain.port.GestorPermisos
import pe.edu.upeu.pharmamobile2.domain.port.MonitorConectividad
import pe.edu.upeu.pharmamobile2.domain.port.NotificadorSincronizacion
import pe.edu.upeu.pharmamobile2.domain.port.Telemetria
import pe.edu.upeu.pharmamobile2.platform.MonitorConectividadAndroid

import pe.edu.upeu.pharmamobile2.data.remote.ApiConfig

actual val platformModule: Module = module {
    single<MonitorConectividad> { MonitorConectividadAndroid(androidContext()) }
    single<HttpClientEngine> { OkHttp.create() }
    single(named("baseUrl")) { ApiConfig.URL_EMULADOR_ANDROID }
    single<Compartidor> { CompartidorAndroid(androidContext()) }
    single<AlmacenSeguro> { AlmacenSeguroAndroid(androidContext()) }
    single<CreadorDeDriver> { CreadorDeDriverAndroid(androidContext()) }
    single<GestorPermisos> { GestorPermisosAndroid(androidContext()) }
    single<NotificadorSincronizacion> { NotificadorAndroid(androidContext(), get()) }
    single<Telemetria> { TelemetriaSegura(TelemetriaFirebase()) }
}
