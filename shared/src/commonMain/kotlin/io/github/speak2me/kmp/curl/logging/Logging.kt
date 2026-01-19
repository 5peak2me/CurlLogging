/*
 * Copyright © 2025 J!nl!n™ Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("INVISIBLE_REFERENCE")

package io.github.speak2me.kmp.curl.logging

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.ClientHook
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.split
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.KtorDsl
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.charsets.MalformedInputException
import io.ktor.utils.io.charsets.decode
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.writeFully
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.io.Buffer

private val ClientCallLogger = AttributeKey<HttpClientCallLogger>("CallLogger")
private val DisableLogging = AttributeKey<Unit>("DisableLogging")

/**
 * A configuration for the [CurlLogging] plugin.
 */
@KtorDsl
public class LoggingConfig {
    internal var filters = mutableListOf<(HttpRequestBuilder) -> Boolean>()
    internal val sanitizedHeaders = mutableListOf<SanitizedHeader>()

    private var _logger: Logger? = null

    /**
     * Specifies a [Logger] instance.
     */
    public var logger: Logger
        get() = _logger ?: Logger.SIMPLE
        set(value) {
            _logger = value
        }

    /**
     * Allows you to filter log messages for calls matching a [predicate].
     */
    public fun filter(predicate: (HttpRequestBuilder) -> Boolean) {
        filters.add(predicate)
    }

    /**
     * Allows you to sanitize sensitive headers to avoid their values appearing in the logs.
     * In the example below, Authorization header value will be replaced with '***' when logging:
     * ```kotlin
     * sanitizeHeader { header -> header == HttpHeaders.Authorization }
     * ```
     */
    public fun sanitizeHeader(placeholder: String = "***", predicate: (String) -> Boolean) {
        sanitizedHeaders.add(SanitizedHeader(placeholder, predicate))
    }
}

/**
 * A client's plugin that provides the capability to log HTTP calls.
 *
 * You can learn more from [Logging](https://ktor.io/docs/client-logging.html).
 */
@OptIn(InternalAPI::class, DelicateCoroutinesApi::class)
public val CurlLogging: ClientPlugin<LoggingConfig> = createClientPlugin("CurlLogging", ::LoggingConfig) {
    val logger: Logger = pluginConfig.logger
    val filters: List<(HttpRequestBuilder) -> Boolean> = pluginConfig.filters
    val sanitizedHeaders: List<SanitizedHeader> = pluginConfig.sanitizedHeaders

    fun shouldBeLogged(request: HttpRequestBuilder): Boolean = filters.isEmpty() || filters.any { it(request) }

    /**
     * Detects if the body is a binary data
     * @return
     * Boolean: true if the body is a binary data.
     * Long?: body size if calculated.
     * ByteReadChannel: body channel with the original data.
     */
    suspend fun detectIfBinary(
        body: ByteReadChannel,
        contentLength: Long?,
        contentType: ContentType?,
        headers: Headers,
    ): Triple<Boolean, Long?, ByteReadChannel> {
        if (headers.contains(HttpHeaders.ContentEncoding)) {
            return Triple(true, contentLength, body)
        }

        val charset = if (contentType != null) {
            contentType.charset() ?: Charsets.UTF_8
        } else {
            Charsets.UTF_8
        }

        var isBinary = false
        val firstChunk = ByteArray(1024)
        val firstReadSize = body.readAvailable(firstChunk)

        if (firstReadSize < 1) {
            return Triple(false, 0L, body)
        }

        val buffer = Buffer().apply { writeFully(firstChunk, 0, firstReadSize) }

        val firstChunkText = try {
            charset.newDecoder().decode(buffer)
        } catch (_: MalformedInputException) {
            isBinary = true
            ""
        }

        if (!isBinary) {
            var lastCharIndex = -1
            firstChunkText.forEach { _ ->
                lastCharIndex += 1
            }

            for ((i, ch) in firstChunkText.withIndex()) {
                if (ch == '\ufffd' && i != lastCharIndex) {
                    isBinary = true
                    break
                }
            }
        }

        if (!isBinary) {
            val channel = ByteChannel()

            val copied = client.async {
                channel.writeFully(firstChunk, 0, firstReadSize)
                val copied = body.copyTo(channel)
                channel.flushAndClose()
                copied
            }.await()

            return Triple(isBinary, copied + firstReadSize, channel)
        }

        return Triple(isBinary, contentLength, body)
    }

    suspend fun logRequestBody(
        content: OutgoingContent,
        contentLength: Long?,
        headers: Headers,
        method: HttpMethod,
        logLines: MutableList<String>,
        body: ByteReadChannel,
    ) {
        val (isBinary, size, newBody) = detectIfBinary(body, contentLength, content.contentType, headers)

        if (!isBinary) {
            val contentType = content.contentType
            val charset = if (contentType != null) {
                contentType.charset() ?: Charsets.UTF_8
            } else {
                Charsets.UTF_8
            }

            logLines.add(newBody.readRemaining().readText(charset = charset))
            logLines.add("--> END ${method.value} ($size-byte body)")
        } else {
            var type = "binary"
            if (headers.contains(HttpHeaders.ContentEncoding)) {
                type = "encoded"
            }

            if (size != null) {
                logLines.add("--> END ${method.value} ($type $size-byte body omitted)")
            } else {
                logLines.add("--> END ${method.value} ($type body omitted)")
            }
        }
    }

    suspend fun logOutgoingContent(
        content: OutgoingContent,
        method: HttpMethod,
        headers: Headers,
        logLines: MutableList<String>,
        process: (ByteReadChannel) -> ByteReadChannel = { it },
    ): OutgoingContent? = when (content) {
        is OutgoingContent.ByteArrayContent -> {
            val bytes = content.bytes()
            logRequestBody(content, bytes.size.toLong(), headers, method, logLines, ByteReadChannel(bytes))
            null
        }

        is OutgoingContent.ContentWrapper -> {
            logOutgoingContent(content.delegate(), method, headers, logLines, process)
        }

        is OutgoingContent.NoContent -> {
            logLines.add("--> END ${method.value}")
            null
        }

        is OutgoingContent.ProtocolUpgrade -> {
            logLines.add("--> END ${method.value}")
            null
        }

        is OutgoingContent.ReadChannelContent -> {
            val (origChannel, newChannel) = content.readFrom().split(client)
            logRequestBody(content, content.contentLength, headers, method, logLines, newChannel)
            LoggedContent(content, origChannel)
        }

        is OutgoingContent.WriteChannelContent -> {
            val channel = ByteChannel()

            client.launch {
                content.writeTo(channel)
                channel.close()
            }

            val (origChannel, newChannel) = channel.split(client)
            logRequestBody(content, content.contentLength, headers, method, logLines, newChannel)
            LoggedContent(content, origChannel)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun logRequestBody(
        content: OutgoingContent,
        logger: HttpClientCallLogger,
    ): OutgoingContent {
        val requestLog = StringBuilder()
//        requestLog.appendLine("BODY Content-Type: ${content.contentType}")

        val charset = content.contentType?.charset() ?: Charsets.UTF_8

        val channel = ByteChannel()
        GlobalScope.launch(Dispatchers.Default + MDCContext()) {
            try {
                val text = channel.tryReadText(charset) ?: "[request body omitted]"
//                requestLog.appendLine("BODY START")
                if (text.isNotEmpty()) requestLog.appendLine(" --data '${text.replace(Regex("\\s+"), "")}'")
//                requestLog.append("BODY END")
            } finally {
                logger.logRequest(requestLog.toString())
                logger.closeRequestLog()
            }
        }

        return content.observe(channel)
    }

    fun logRequestException(context: HttpRequestBuilder, cause: Throwable) {
        logger.log("REQUEST ${Url(context.url)} failed with exception: $cause")
    }

    suspend fun logRequest(request: HttpRequestBuilder): OutgoingContent? {
        val content = request.body as OutgoingContent
        val callLogger = HttpClientCallLogger(logger)
        request.attributes.put(ClientCallLogger, callLogger)

        val message = buildString {
            append("curl -X ${request.method} ${Url(request.url)}")

            // headers
            logHeaders(request.headers.entries(), sanitizedHeaders)

//            val contentLengthPlaceholder = sanitizedHeaders
//                .firstOrNull { it.predicate(HttpHeaders.ContentLength) }
//                ?.placeholder
            val contentTypePlaceholder = sanitizedHeaders
                .firstOrNull { it.predicate(HttpHeaders.ContentType) }
                ?.placeholder
            content.contentLength?.takeIf { it > 0 }?.let {
//                logHeader(HttpHeaders.ContentLength, contentLengthPlaceholder ?: it.toString())
            }
            content.contentType?.let {
                logHeader(HttpHeaders.ContentType, contentTypePlaceholder ?: it.toString())
            }
            logHeaders(content.headers.entries(), sanitizedHeaders)
        }

        if (message.isNotEmpty()) {
            callLogger.logRequest(message)
        }

        if (message.isEmpty()) {
            callLogger.closeRequestLog()
            return null
        }

        return logRequestBody(content, callLogger)
    }

    on(SendHook) { request ->
        if (!shouldBeLogged(request)) {
            request.attributes.put(DisableLogging, Unit)
            return@on
        }

        val loggedRequest = try {
            logRequest(request)
        } catch (_: Throwable) {
            null
        }

        try {
            proceedWith(loggedRequest ?: request.body)
        } catch (cause: Throwable) {
            logRequestException(request, cause)
            throw cause
        } finally {
        }
    }
}

private fun computeRequestBodySize(content: Any): Long {
    check(content is OutgoingContent)

    return when (content) {
        is OutgoingContent.ByteArrayContent -> content.bytes().size.toLong()
        is OutgoingContent.ContentWrapper -> computeRequestBodySize(content.delegate())
        is OutgoingContent.NoContent -> 0
        is OutgoingContent.ProtocolUpgrade -> 0
        else -> error("Unable to calculate the size for type ${content::class.simpleName}")
    }
}

/**
 * Configures and installs [CurlLogging] in [HttpClient].
 */
@Suppress("FunctionName")
public fun HttpClientConfig<*>.CurlLogging(block: LoggingConfig.() -> Unit = {}) {
    install(CurlLogging, block)
}

internal class SanitizedHeader(
    val placeholder: String,
    val predicate: (String) -> Boolean,
)

private object SendHook :
    ClientHook<suspend SendHook.Context.(response: HttpRequestBuilder) -> Unit> {

    class Context(private val context: PipelineContext<Any, HttpRequestBuilder>) {
        suspend fun proceedWith(content: Any) = context.proceedWith(content)
    }

    override fun install(
        client: HttpClient,
        handler: suspend Context.(request: HttpRequestBuilder) -> Unit,
    ) {
        client.sendPipeline.intercept(HttpSendPipeline.Monitoring) {
            handler(Context(this), context)
        }
    }
}
