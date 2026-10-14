package pe.edu.upeu.pharmamobile2.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProviders
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import pe.edu.upeu.pharmamobile2.data.local.AlmacenSeguro

fun createHttpClient(
    engine: HttpClientEngine,
    almacenSeguro: AlmacenSeguro,
    baseUrl: String = "http://10.0.2.2:8080/"
): HttpClient =
    HttpClient(engine) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
        }

        install(Auth) {
            bearer {
                loadTokens {
                    val token = almacenSeguro.obtenerToken()
                    if (!token.isNullOrBlank()) {
                        BearerTokens(accessToken = token, refreshToken = "")
                    } else {
                        null
                    }
                }
                sendWithoutRequest { request ->
                    !request.url.encodedPath.contains("auth/login")
                }
            }
        }

        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    println(message)
                }
            }
            level = LogLevel.HEADERS
            sanitizeHeader { header ->
                header.equals(HttpHeaders.Authorization, ignoreCase = true)
            }
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

fun HttpClient.limpiarTokensAuth() {
    runCatching {
        authProviders
            .filterIsInstance<BearerAuthProvider>()
            .forEach { it.clearToken() }
    }
}
