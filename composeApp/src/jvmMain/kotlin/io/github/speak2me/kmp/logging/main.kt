package io.github.speak2me.kmp.logging

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "CurlLogging",
    ) {
        App()
    }
}