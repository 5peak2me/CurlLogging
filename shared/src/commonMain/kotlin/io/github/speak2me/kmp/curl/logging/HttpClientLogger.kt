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
package io.github.speak2me.kmp.curl.logging

import io.ktor.client.plugins.logging.Logger
//import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Job
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
internal class HttpClientCallLogger(private val logger: Logger) {
    private val requestLog = StringBuilder()
    private val requestLoggedMonitor = Job()

    private val requestLogged = AtomicBoolean(false)

    fun logRequest(message: String) {
        requestLog.append(message.trimEnd())
    }

    fun closeRequestLog() {
        if (!requestLogged.compareAndSet(expectedValue = false, newValue = true)) return

        try {
            val message = requestLog.trim().toString()
            if (message.isNotEmpty()) {
                logger.log(message)
            }
        } finally {
            requestLoggedMonitor.complete()
        }
    }
}