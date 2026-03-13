package com.brytebee.ecomesh.core.discovery

actual fun getPlatformDiscoveryServices(): List<DiscoveryService> {
    // BLE implementation for iOS will be added later
    return emptyList()
}
