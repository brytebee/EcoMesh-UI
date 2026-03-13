package com.brytebee.ecomesh.core.account

/**
 * Represents a user's account tier in the EcoMesh ecosystem.
 *
 * During Phase 1–6, [isMidTier] and [isPro] default to true so all features
 * are testable without a payment gateway. In Phase 7, these flags will be
 * driven by the real LocalCreditLedger / NIBSS payment verification.
 *
 * Tiers:
 *  - Free:     unlimited text, basic Eco Mode (auto @ 42°C)
 *  - Mid-Tier: text + photos + audio + 10MB file transfers + priority relay
 *  - Pro:      unlimited files + custom thermal thresholds + storefront + desktop mirroring
 */
data class UserAccount(
    val userId: String,
    val username: String,

    // TODO (Phase 7): Replace with real license verification via LocalCreditLedger
    val isMidTier: Boolean = true,
    val isPro: Boolean = true,
) {
    val tier: AccountTier
        get() = when {
            isPro -> AccountTier.PRO
            isMidTier -> AccountTier.MID
            else -> AccountTier.FREE
        }
}

enum class AccountTier {
    /** Text-only messaging, automated Eco Mode at 42°C */
    FREE,

    /** Text + photos + audio + 10MB file transfers, priority relay — ₦300/month */
    MID,

    /** Unlimited files, custom thermal thresholds, storefront, desktop mirroring — ₦700/month */
    PRO;

    fun canTransferFiles(): Boolean = this != FREE
    fun canTransferLargeFiles(): Boolean = this == PRO
    fun hasCustomEcoMode(): Boolean = this == PRO
    fun hasVerifiedStorefront(): Boolean = this == PRO
    fun hasDesktopMirroring(): Boolean = this == PRO
}
