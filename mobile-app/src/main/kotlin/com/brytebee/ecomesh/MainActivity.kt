package com.brytebee.ecomesh

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.brytebee.ecomesh.core.discovery.AndroidContext
import com.brytebee.ecomesh.core.discovery.NetworkStateHolder
import com.brytebee.ecomesh.ui.App

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions result — scan will auto-retry via DiscoveryManager */ }

    /**
     * Listens for WiFi and Bluetooth ON/OFF events at the OS level.
     * Updates NetworkStateHolder which DiscoveryManager polls reactively.
     */
    private val networkStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)
                    NetworkStateHolder.isWifiEnabled = (state == WifiManager.WIFI_STATE_ENABLED)
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                    NetworkStateHolder.isBluetoothEnabled = (state == BluetoothAdapter.STATE_ON)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidContext.context = applicationContext
        enableEdgeToEdge()
        requestPermissionsIfNecessary()

        // Sync initial state before composables render
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        NetworkStateHolder.isWifiEnabled = wifiManager.isWifiEnabled
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        NetworkStateHolder.isBluetoothEnabled = adapter?.isEnabled == true

        setContent { App() }
    }

    override fun onStart() {
        super.onStart()
        // Start the foreground service to keep mesh connections alive in the background
        val serviceIntent = Intent(this, MeshService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        // Register for live WiFi/BT state changes
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(networkStateReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(networkStateReceiver)
        // Note: We do NOT stop MeshService here — that is intentional.
        // The service lives until the user explicitly disconnects (via the notification action)
        // or the process is killed.
    }

    private fun requestPermissionsIfNecessary() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
        }

        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}
