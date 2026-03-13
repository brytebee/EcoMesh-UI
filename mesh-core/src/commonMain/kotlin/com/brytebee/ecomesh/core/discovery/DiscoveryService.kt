package com.brytebee.ecomesh.core.discovery

import kotlinx.coroutines.flow.Flow

/**
 * Interface for hardware-specific discovery methods (BLE, mDNS, etc.)
 */
interface DiscoveryService {
    /**
     * Start the discovery process.
     */
    fun startDiscovery()

    /**
     * Stop the discovery process.
     */
    fun stopDiscovery()

    /**
     * Stream of currently available peers found by this service.
     */
    val peers: Flow<List<Peer>>
}
