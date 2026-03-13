package com.brytebee.ecomesh.core.transport

actual fun getPlatformTransportService(): TransportService {
    return object : TransportService {
        override suspend fun connect(peerId: String) = false
        override suspend fun disconnect() {}
        override suspend fun sendData(data: ByteArray) = false
        override val incomingData = kotlinx.coroutines.flow.emptyFlow<ByteArray>()
    }
}
