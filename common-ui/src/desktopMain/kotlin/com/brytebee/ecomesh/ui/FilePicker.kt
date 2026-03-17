package com.brytebee.ecomesh.ui

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser

@Composable
actual fun rememberFilePicker(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    return {
        // JFileChooser blocks the current thread, so this is simplistic. 
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Select File to Send"
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            try {
                val bytes = file.readBytes()
                onResult(bytes, file.name)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null, null)
            }
        } else {
            onResult(null, null)
        }
    }
}

actual fun openFile(fileName: String) {
    try {
        val file = java.io.File("downloads/$fileName")
        if (file.exists() && java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(file)
        } else {
            println("EcoMesh: File not found or Desktop not supported for $fileName")
        }
    } catch (e: Exception) {
        println("EcoMesh: Error opening file $fileName - ${e.message}")
        e.printStackTrace()
    }
}
