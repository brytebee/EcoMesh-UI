package com.brytebee.ecomesh.ui

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    return {
        // TODO: Implement UIDocumentPickerViewController for iOS
        println("EcoMesh: File picker not yet implemented on iOS")
        onResult(null, null)
    }
}

actual fun openFile(fileName: String) {
    println("EcoMesh: openFile not yet implemented on iOS")
}
