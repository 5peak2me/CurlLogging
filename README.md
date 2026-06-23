# CurlLogging [![Version](https://img.shields.io/badge/Version-0.0.1-orange)](https://github.com/5peak2me/CurlLogging/releases)

[![Ktor](https://img.shields.io/badge/dynamic/toml?url=https://raw.githubusercontent.com/5peak2me/CurlLogging/main/gradle/libs.versions.toml&query=%24.versions.ktor&label=Kotlin&color=blue&logo=ktor)](https://ktor.io)
[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https://raw.githubusercontent.com/5peak2me/CurlLogging/main/gradle/libs.versions.toml&query=%24.versions.kotlin&label=Kotlin&color=blue&logo=kotlin)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/dynamic/toml?url=https://raw.githubusercontent.com/5peak2me/CurlLogging/main/gradle/libs.versions.toml&query=%24.versions.agp&label=AGP&color=blue&logo=android)](https://developer.android.com/build/releases/gradle-plugin)
[![Gradle](https://img.shields.io/badge/dynamic/regex?url=https%3A%2F%2Fraw.githubusercontent.com%2F5peak2me%2FCurlLogging%2Fmain%2Fgradle%2Fwrapper%2Fgradle-wrapper.properties&search=gradle-%28%5B0-9.%5D%2B%29-%28%3F%3Abin%7Call%29%5C.zip&replace=%241&label=Gradle&color=blue&logo=gradle&logoColor=white)](https://gradle.org)

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
