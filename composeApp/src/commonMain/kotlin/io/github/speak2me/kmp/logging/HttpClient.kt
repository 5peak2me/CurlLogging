package io.github.speak2me.kmp.logging

import io.github.speak2me.kmp.curl.logging.CurlLogging
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingFormat
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

internal expect fun getHttpClient(): HttpClient

private const val TIME_OUT = 30_000L

internal fun HttpClientConfig<*>.defaultHttpClientConfig() {
    defaultRequest {
        url("https://httpbin.org")
        contentType(ContentType.Application.Json) // header(HttpHeaders.ContentType, ContentType.Application.Json)
    }
    install(HttpTimeout) {
        socketTimeoutMillis = TIME_OUT
        requestTimeoutMillis = TIME_OUT
        connectTimeoutMillis = TIME_OUT
    }
    install(Logging) {
//        format = LoggingFormat.OkHttp
        logger = object : Logger {
            override fun log(message: String) {
                println("[Http]: $message")
            }
        }
        level = LogLevel.ALL
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
    install(CurlLogging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("╭──────╮ \n│ cURL │: $message\n╰──────╯ ️")
            }
        }
        filter {
            true
        }
        sanitizeHeader {
            false
        }
    }
//    install(ContentNegotiation) {
//        json(
//            Json {
//                serializersModule = SerializersModule {
//                    contextual(LocalDate::class, DefaultFlexibleDate)
//                }
//                prettyPrint = true
//                isLenient = true
//                ignoreUnknownKeys = true
//                explicitNulls = false
//            },
//        )
//    }
//    HttpResponseValidator {
//        validateResponse {
//
//        }
//        handleResponseException { cause, request ->
//
//        }
//        handleResponseExceptionWithRequest { cause, request ->
//
//        }
//    }

}
