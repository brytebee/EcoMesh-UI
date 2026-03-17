package com.brytebee.ecomesh.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    return {
        // TODO: Implement HTML input type=file for WebAssembly
        println("EcoMesh: File picker not yet implemented on Wasm")
        onResult(null, null)
    }
}

actual fun openFile(fileName: String) {
    println("EcoMesh: openFile not yet implemented on Wasm")
}
