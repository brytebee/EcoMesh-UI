package com.brytebee.ecomesh.core.discovery

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Represents a nearby device discovered via the mesh network.
 */
data class Peer(
    val id: String,
    val name: String,
    val type: PeerType,
    val lastSeen: Instant = Clock.System.now(),
    val status: PeerStatus = PeerStatus.AVAILABLE,
    val rssi: Int? = null // Signal strength indicator
)

enum class PeerType {
    MOBILE,
    DESKTOP
}

enum class PeerStatus {
    OFFLINE,
    AVAILABLE,
    CONNECTING,
    CONNECTED
}
