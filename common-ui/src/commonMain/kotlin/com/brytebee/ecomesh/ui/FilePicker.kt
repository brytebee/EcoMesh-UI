package com.brytebee.ecomesh.ui

import androidx.compose.runtime.Composable

/**
 * Platform-independent file picker that returns a trigger function.
 * When the trigger is called, platform-specific UI is shown.
 * Result contains the absolute file path and its file name.
 */
@Composable
expect fun rememberFilePicker(onResult: (String?, String?) -> Unit): () -> Unit

/**
 * Attempts to open a received file using the platform's default application.
 */
expect fun openFile(fileName: String, filePath: String? = null)
