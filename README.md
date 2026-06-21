# CurlLogging

[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF?logo=kotlin)](https://kotlinlang.org)
[![Ktor](https://img.shields.io/badge/Ktor-3.5.0-087CFA)](https://ktor.io)
[![Version](https://img.shields.io/badge/Version-0.0.1-orange)](https://github.com/5peak2me/CurlLogging/releases)
[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Introduction

CurlLogging is a Kotlin Multiplatform Ktor client plugin that logs outgoing requests as reproducible cURL commands.

It is useful when you need to copy an app request into a terminal, share a failing request with another developer, or compare Ktor client behavior with a raw HTTP call.

## Features

- Generates cURL commands from outgoing Ktor client requests.
- Logs request method, URL, headers, content type, and request body.
- Supports request filtering and sensitive header sanitization.

## Quick Start

Add Maven Central:

```kotlin
repositories {
    mavenCentral()
}
```

Add the dependency to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.5peak2me.kmp:curl-logging:0.0.1")
        }
    }
}
```

Install `CurlLogging` in your Ktor `HttpClient`:

```kotlin
import io.github.speak2me.kmp.curl.logging.CurlLogging
import io.ktor.client.HttpClient

val client = HttpClient {
    install(CurlLogging)
}
```

Example output:

```shell
curl -X GET 'https://httpbin.org/get' -H 'Accept: */*' -H 'Accept-Charset: UTF-8' -H 'Content-Type: application/json'
```

## Configuration

Use a custom logger when you want to route commands to your own logging system:

```kotlin
import io.github.speak2me.kmp.curl.logging.CurlLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logger

val client = HttpClient {
    install(CurlLogging) {
        logger = object : Logger {
            override fun log(message: String) {
                println(message)
            }
        }
    }
}
```

Filter which requests are logged:

```kotlin
install(CurlLogging) {
    filter { request ->
        request.url.host == "api.example.com"
    }
}
```

Sanitize sensitive headers:

```kotlin
import io.ktor.http.HttpHeaders

install(CurlLogging) {
    sanitizeHeader { header ->
        header == HttpHeaders.Authorization
    }
}
```

## Development

Run a quick JVM compile check:

```shell
./gradlew :shared:compileKotlinJvm
```

On Windows:

```shell
.\gradlew.bat :shared:compileKotlinJvm
```

## License

[![License](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
