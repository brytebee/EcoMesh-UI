package com.brytebee.ecomesh.core.discovery

actual fun getPlatformDiscoveryServices(): List<DiscoveryService> {
    // Wasm typically doesn't have local BLE/mDNS access
    return emptyList()
}
