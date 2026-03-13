package com.brytebee.ecomesh.core.transport

import com.brytebee.ecomesh.core.discovery.AndroidContext

actual fun getPlatformTransportService(): TransportService {
    return AndroidWifiDirectTransport(AndroidContext.context)
}
