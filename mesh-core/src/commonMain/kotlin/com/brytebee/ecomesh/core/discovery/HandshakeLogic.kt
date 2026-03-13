package com.brytebee.ecomesh.core.discovery

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Lightweight handshake packet to exchange identity and capabilities.
 */
@Serializable
data class HandshakePacket(
    val senderName: String,
    val senderType: String,
    val isPro: Boolean,
    val timestamp: Long
)

object HandshakeLogic {
    private val json = Json { ignoreUnknownKeys = true }

    fun createPacket(name: String, type: String, isPro: Boolean): String {
        return json.encodeToString(
            HandshakePacket(name, type, isPro, System.currentTimeMillis())
        )
    }

    fun parsePacket(data: String): HandshakePacket? {
        return try {
            json.decodeFromString<HandshakePacket>(data)
        } catch (e: Exception) {
            null
        }
    }
}
