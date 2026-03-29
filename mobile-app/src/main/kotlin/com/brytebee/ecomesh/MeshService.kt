package com.brytebee.ecomesh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Android Foreground Service that keeps the application's coroutine scope and TCP socket
 * alive while the app is minimized or the screen is off.
 *
 * Without this, Android's process priority drops when the app enters the background, and
 * the OS kills the coroutine scope — dropping all active TCP connections and sessions.
 *
 * This service acquires a persistent notification slot, which prevents out-of-memory kills
 * for as long as the mesh is active.
 */
class MeshService : Service() {

    companion object {
        const val CHANNEL_ID = "ecomesh_mesh_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.brytebee.ecomesh.action.START_MESH"
        const val ACTION_STOP  = "com.brytebee.ecomesh.action.STOP_MESH"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        }
        return START_STICKY // Restart if killed by the system
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, MeshService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EcoMesh Active")
            .setContentText("Secure mesh network is running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", stopPending)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "EcoMesh Network",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the secure EcoMesh connection alive in the background"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
