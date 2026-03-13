package com.brytebee.ecomesh.core.discovery

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class AndroidDiscoveryService(
    private val context: Context,
    private val serviceUuid: UUID = UUID.fromString("0000e00-0000-1000-8000-00805f9b34fb") // EcoMesh UUID
) : DiscoveryService {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val advertiser = adapter.bluetoothLeAdvertiser
    private val scanner = adapter.bluetoothLeScanner

    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    override val peers: StateFlow<List<Peer>> = _peers

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val peerName = result.scanRecord?.deviceName ?: device.address ?: "Unknown"
            val peer = Peer(
                id = device.address,
                name = peerName,
                type = PeerType.MOBILE,
                rssi = result.rssi
            )
            updatePeerList(peer)
        }

        override fun onScanFailed(errorCode: Int) {
            // Log scan failure
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            // Advertising started
        }
        override fun onStartFailure(errorCode: Int) {
            // Log advertise failure
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

    @SuppressLint("MissingPermission")
    override fun startDiscovery() {
        if (!adapter.isEnabled) return

        // 1. Start Scanning
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(serviceUuid))
            .build()
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner.startScan(listOf(filter), scanSettings, scanCallback)

        // 2. Start Advertising
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(serviceUuid))
            .build()
        advertiser.startAdvertising(settings, data, advertiseCallback)
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        scanner.stopScan(scanCallback)
        advertiser.stopAdvertising(advertiseCallback)
    }
}
