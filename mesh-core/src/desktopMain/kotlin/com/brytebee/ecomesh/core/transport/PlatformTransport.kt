package com.brytebee.ecomesh.core.transport

actual fun getPlatformTransportService(): TransportService {
    return DesktopSocketTransport()
}
