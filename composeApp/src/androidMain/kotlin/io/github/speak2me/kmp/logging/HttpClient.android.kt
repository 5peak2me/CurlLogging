package io.github.speak2me.kmp.logging

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.Dispatchers

internal actual fun getHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        dispatcher = Dispatchers.IO.limitedParallelism(8)

        config {
            followRedirects(true)
            followSslRedirects(true)
        }
    }
    defaultHttpClientConfig()
}