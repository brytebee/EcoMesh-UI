package com.brytebee.ecomesh.core.discovery

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Orchestrates multiple discovery services and provides a unified view of nearby peers.
 */
class DiscoveryManager(
    private val services: List<DiscoveryService>,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _peers = MutableStateFlow<Map<String, Peer>>(emptyMap())
    val peers: StateFlow<List<Peer>> = _peers
        .map { it.values.toList().sortedByDescending { p -> p.lastSeen } }
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    fun start() {
        services.forEach { it.startDiscovery() }
        
        services.forEach { service ->
            scope.launch {
                service.peers.collect { incomingPeers ->
                    updatePeers(incomingPeers)
                }
            }
        }
    }

    private fun updatePeers(newList: List<Peer>) {
        val current = _peers.value.toMutableMap()
        newList.forEach { peer ->
            current[peer.id] = peer
        }
        _peers.value = current
    }

    fun stop() {
        services.forEach { it.stopDiscovery() }
    }
}
