package com.brytebee.ecomesh.ui

import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.brytebee.ecomesh.core.discovery.AndroidContext
import com.brytebee.ecomesh.core.messaging.downloadsDir
import kotlinx.coroutines.launch
import java.io.File

@Composable
actual fun rememberFilePicker(onResult: (String?, String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) {
            onResult(null, null)
            return@rememberLauncherForActivityResult
        }
        
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
        
        // Return the content URI string directly — no local cache copying!
        // This completely eliminates the OutOfMemory/DiskSpace crash for massive 2GB+ files.
        onResult(uri.toString(), fileName)
    }
    
    return { launcher.launch("*/*") }
}

actual fun openFile(fileName: String, filePath: String?) {
    try {
        val context = AndroidContext.context
        
        // If the SENDER tries to open a file they picked on Android, it will be a content:// URI
        if (filePath != null && filePath.startsWith("content://")) {
            val uri = android.net.Uri.parse(filePath)
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        }

        // For received files, or absolute file paths
        val file = if (filePath != null && File(filePath).exists()) {
            File(filePath)
        } else {
            File("$downloadsDir/$fileName")
        }

        if (!file.exists()) {
            println("EcoMesh: File not found at ${file.absolutePath}")
            return
        }
        val ext = fileName.substringAfterLast('.', "").lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        println("EcoMesh: Error opening file $fileName - ${e.message}")
        e.printStackTrace()
    }
}

