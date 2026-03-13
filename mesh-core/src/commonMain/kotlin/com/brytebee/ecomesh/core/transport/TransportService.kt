package com.brytebee.ecomesh.core.transport

import kotlinx.coroutines.flow.Flow

/**
 * Interface for transferring data between peers after discovery.
 */
interface TransportService {
    /**
     * Connect to a specific peer.
     */
    suspend fun connect(peerId: String): Boolean

    /**
     * Disconnect from the current peer.
     */
    suspend fun disconnect()

    /**
     * Send a raw byte array to the connected peer.
     */
    suspend fun sendData(data: ByteArray): Boolean

    /**
     * Stream of incoming data from the connected peer.
     */
    val incomingData: Flow<ByteArray>
}
