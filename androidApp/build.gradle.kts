import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

// Credenciales de firma: viven en keystore.properties (raiz, ignorado por Git).
// Si el archivo no existe, el proyecto abre y compila en debug; solo el release queda sin firmar.
val firma = Properties().apply {
    val archivo = rootProject.file("keystore.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val firmaDisponible = firma.getProperty("storeFile")?.let { file(it).exists() } == true

// URL del backend: local.properties (api.baseUrl, ignorado por Git). Por defecto, el host del emulador.
// En un telefono fisico debe ser la IP de la maquina en la red local, por ejemplo http://192.168.1.40:8080/
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val apiBaseUrl: String = propiedadesLocales.getProperty("api.baseUrl") ?: "http://10.0.2.2:8080/"
// Si el servicio va por HTTP, solo ese host se autoriza para trafico sin cifrar (HTTPS no necesita excepcion).
val hostBackendHttp: String = URI(apiBaseUrl).takeIf { it.scheme == "http" }?.host.orEmpty()

abstract class GenerarConfigRed : DefaultTask() {
    @get:Input abstract val hostHttp: Property<String>
    @get:Input abstract val permitirEmulador: Property<Boolean>
    @get:OutputDirectory abstract val salida: DirectoryProperty

    @TaskAction
    fun generar() {
        val hosts = buildList {
            if (hostHttp.get().isNotBlank()) add(hostHttp.get())
            if (permitirEmulador.get()) add("10.0.2.2")
        }.distinct()
        val dominios = hosts.joinToString(separator = System.lineSeparator()) { host ->
            """    <domain-config cleartextTrafficPermitted="true"><domain includeSubdomains="false">$host</domain></domain-config>"""
        }
        val destino = salida.get().asFile.resolve("xml").also { it.mkdirs() }
        destino.resolve("network_security_config.xml").writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por Gradle a partir de api.baseUrl. No editar: se regenera en cada build. -->
<network-security-config>
    <base-config cleartextTrafficPermitted="false" />
$dominios
</network-security-config>
"""
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.firebase.crashlytics)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "pe.edu.upeu.pharmamobile2"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.edu.upeu.pharmamobile2"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
    signingConfigs {
        if (firmaDisponible) {
            create("release") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            if (firmaDisponible) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

androidComponents {
    onVariants { variante ->
        val generar = tasks.register<GenerarConfigRed>("generarConfigRed${variante.name.replaceFirstChar { it.uppercase() }}") {
            hostHttp.set(hostBackendHttp)
            permitirEmulador.set(variante.buildType == "debug")
        }
        variante.sources.res?.addGeneratedSourceDirectory(generar, GenerarConfigRed::salida)
    }
}
