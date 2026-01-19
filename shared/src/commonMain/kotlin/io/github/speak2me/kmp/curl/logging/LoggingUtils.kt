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

internal fun Appendable.logHeaders(
    headers: Set<Map.Entry<String, List<String>>>,
    sanitizedHeaders: List<SanitizedHeader>,
) {
    val sortedHeaders = headers.toList().sortedBy { it.key }

    sortedHeaders.forEach { (key, values) ->
        val placeholder = sanitizedHeaders.firstOrNull { it.predicate(key) }?.placeholder
        logHeader(key, placeholder ?: values.joinToString("; "))
    }
}

internal fun Appendable.logHeader(key: String, value: String) {
    append(" -H '$key: $value'")
}
