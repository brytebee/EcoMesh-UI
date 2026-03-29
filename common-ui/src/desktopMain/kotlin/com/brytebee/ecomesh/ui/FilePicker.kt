package com.brytebee.ecomesh.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

@Composable
actual fun rememberFilePicker(onResult: (String?, String?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch(Dispatchers.Default) {
            // JFileChooser must run on the Swing EDT, not the Compose thread
            var selectedPath: String? = null
            var selectedName: String? = null
            withContext(Dispatchers.Main) {
                val fileChooser = JFileChooser()
                fileChooser.dialogTitle = "Select File to Send"
                val result = fileChooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    val file = fileChooser.selectedFile
                    try {
                        selectedPath = file.absolutePath
                        selectedName = file.name
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            onResult(selectedPath, selectedName)
        }
    }
}

actual fun openFile(fileName: String, filePath: String?) {
    try {
        // First try the absolute path if provided (useful when the user is the sender)
        if (filePath != null) {
            val directFile = java.io.File(filePath)
            if (directFile.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(directFile)
                return
            }
        }
        
        // Fallback: Use the platform downloadsDir to find the saved file
        val downloadsPath = com.brytebee.ecomesh.core.messaging.downloadsDir
        val file = java.io.File("$downloadsPath/$fileName")
        if (file.exists() && java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(file)
        } else {
            // Additional fallback: try relative path for backward compat
            val fallback = java.io.File("downloads/$fileName")
            if (fallback.exists() && java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(fallback)
            } else {
                println("EcoMesh: File not found at $downloadsPath/$fileName or downloads/$fileName")
            }
        }
    } catch (e: Exception) {
        println("EcoMesh: Error opening file $fileName - ${e.message}")
        e.printStackTrace()
    }
}
