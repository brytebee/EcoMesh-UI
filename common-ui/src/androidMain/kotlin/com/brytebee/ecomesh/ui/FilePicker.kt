package com.brytebee.ecomesh.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFilePicker(onResult: (ByteArray?, String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            onResult(null, null)
            return@rememberLauncherForActivityResult
        }
        
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes != null && fileName != null) {
                onResult(bytes, fileName)
            } else {
                onResult(null, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(null, null)
        }
    }
    
    return { launcher.launch("*/*") }
}

actual fun openFile(fileName: String) {
    // Note: To properly open a file on Android, we need the Context. 
    // Since this is called outside a Composable, we might need an application context reference.
    // For now, let's gracefully fail or print a log. A complete solution requires FileProvider and a Content URI.
    println("EcoMesh: openFile requested for $fileName on Android. (Requires FileProvider and Context)")
}
