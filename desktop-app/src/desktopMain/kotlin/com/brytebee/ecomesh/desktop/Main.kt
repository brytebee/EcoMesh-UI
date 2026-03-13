package com.brytebee.ecomesh.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.brytebee.ecomesh.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "EcoMesh",
        state = rememberWindowState(width = 1024.dp, height = 720.dp),
    ) {
        App()
    }
}
