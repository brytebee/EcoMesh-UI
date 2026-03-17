package com.brytebee.ecomesh.ui

import androidx.compose.runtime.Composable

/**
 * Platform-independent file picker that returns a trigger function.
 * When the trigger is called, platform-specific UI is shown.
 * Result contains the raw ByteArray of the file and its file name.
 */
@Composable
expect fun rememberFilePicker(onResult: (ByteArray?, String?) -> Unit): () -> Unit

/**
 * Attempts to open a received file using the platform's default application.
 */
expect fun openFile(fileName: String)
