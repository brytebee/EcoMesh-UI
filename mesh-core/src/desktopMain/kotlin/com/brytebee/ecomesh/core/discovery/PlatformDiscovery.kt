package com.brytebee.ecomesh.core.discovery

actual fun getPlatformDiscoveryServices(): List<DiscoveryService> {
    return listOf(DesktopDiscoveryService())
}
