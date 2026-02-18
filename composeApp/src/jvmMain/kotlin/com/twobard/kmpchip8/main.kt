package com.twobard.kmpchip8

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMPChip8",
    ) {
        App()
    }
}