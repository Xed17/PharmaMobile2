package pe.edu.upeu.pharmamobile2.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguroIos
import pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriver
import pe.edu.upeu.pharmamobile2.data.local.CreadorDeDriverIos
import pe.edu.upeu.pharmamobile2.domain.platform.Compartidor
import pe.edu.upeu.pharmamobile2.platform.CompartidorIos
import pe.edu.upeu.pharmamobile2.data.firebase.TelemetriaIos
import pe.edu.upeu.pharmamobile2.data.firebase.TelemetriaSegura
import pe.edu.upeu.pharmamobile2.data.notificacion.NotificadorIos
import pe.edu.upeu.pharmamobile2.data.permiso.GestorPermisosIos
import pe.edu.upeu.pharmamobile2.domain.port.GestorPermisos
import pe.edu.upeu.pharmamobile2.domain.port.NotificadorSincronizacion
import pe.edu.upeu.pharmamobile2.domain.port.Telemetria

import pe.edu.upeu.pharmamobile2.data.remote.ApiConfig

actual val platformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single(named("baseUrl")) { ApiConfig.URL_SIMULADOR_IOS }
    single<Compartidor> { CompartidorIos() }
    single<AlmacenSeguro> { AlmacenSeguroIos() }
    single<CreadorDeDriver> { CreadorDeDriverIos() }
    single<GestorPermisos> { GestorPermisosIos() }
    single<NotificadorSincronizacion> { NotificadorIos() }
    single<Telemetria> { TelemetriaSegura(TelemetriaIos()) }
}
