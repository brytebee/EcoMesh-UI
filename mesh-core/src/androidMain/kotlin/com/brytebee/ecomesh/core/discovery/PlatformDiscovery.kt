package com.brytebee.ecomesh.core.discovery

import android.content.Context

/**
 * Global holder for Android context to be used in KMP.
 */
object AndroidContext {
    lateinit var context: Context
}

actual fun getPlatformDiscoveryServices(): List<DiscoveryService> {
    return listOf(AndroidDiscoveryService(AndroidContext.context))
}
