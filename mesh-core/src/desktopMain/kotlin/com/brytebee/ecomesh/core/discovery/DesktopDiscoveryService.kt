package com.brytebee.ecomesh.core.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jmdns.JmDNS
import org.jmdns.ServiceEvent
import org.jmdns.ServiceInfo
import org.jmdns.ServiceListener
import java.net.InetAddress

class DesktopDiscoveryService(
    private val serviceType: String = "_ecomesh._tcp.local."
) : DiscoveryService {

    private var jmdns: JmDNS? = null
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    override val peers: StateFlow<List<Peer>> = _peers

    private val serviceListener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent) {
            // Service discovered, now resolve it
            jmdns?.requestServiceInfo(event.type, event.name)
        }

        override fun serviceRemoved(event: ServiceEvent) {
            val current = _peers.value.toMutableList()
            current.removeAll { it.id == event.name }
            _peers.value = current
        }

        override fun serviceResolved(event: ServiceEvent) {
            val info = event.info
            val peer = Peer(
                id = event.name,
                name = event.name,
                type = PeerType.DESKTOP,
                rssi = null // mDNS doesn't typically provide RSSI
            )
            updatePeerList(peer)
        }
    }

    private fun updatePeerList(newPeer: Peer) {
        val current = _peers.value.toMutableList()
        val index = current.indexOfFirst { it.id == newPeer.id }
        if (index != -1) {
            current[index] = newPeer
        } else {
            current.add(newPeer)
        }
        _peers.value = current
    }

    override fun startDiscovery() {
        try {
            val localhost = InetAddress.getLocalHost()
            jmdns = JmDNS.create(localhost)
            
            // 1. Register our own service
            val serviceInfo = ServiceInfo.create(serviceType, "EcoMesh-Desktop", 8080, "EcoMesh Node")
            jmdns?.registerService(serviceInfo)

            // 2. Listen for other services
            jmdns?.addServiceListener(serviceType, serviceListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun stopDiscovery() {
        jmdns?.unregisterAllServices()
        jmdns?.removeServiceListener(serviceType, serviceListener)
        jmdns?.close()
        jmdns = null
    }
}
