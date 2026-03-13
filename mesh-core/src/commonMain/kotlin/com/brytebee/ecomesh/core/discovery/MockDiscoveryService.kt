package com.brytebee.ecomesh.core.discovery

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A mock discovery service that generates random peers for UI testing.
 */
class MockDiscoveryService : DiscoveryService {
    private var isRunning = false

    override fun startDiscovery() {
        isRunning = true
    }

    override fun stopDiscovery() {
        isRunning = false
    }

    override val peers: Flow<List<Peer>> = flow {
        while (true) {
            if (isRunning) {
                emit(
                    listOf(
                        Peer("peer-1", "Bolu's Laptop", PeerType.DESKTOP, rssi = -65),
                        Peer("peer-2", "Favour's Phone", PeerType.MOBILE, rssi = -42),
                        Peer("peer-3", "Hostel-Node-Alpha", PeerType.DESKTOP, rssi = -80)
                    )
                )
            } else {
                emit(emptyList())
            }
            delay(5000)
        }
    }
}
