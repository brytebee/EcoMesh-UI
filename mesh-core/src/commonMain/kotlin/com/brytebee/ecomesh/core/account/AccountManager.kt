package com.brytebee.ecomesh.core.account

/**
 * Manages the current user's account tier.
 *
 * Phase 1–6: Returns a stubbed account (all tiers unlocked) so every feature
 *             can be developed without a payment system in place.
 *
 * Phase 7:   Replace [getAccount] to verify against the LocalCreditLedger
 *             and NIBSS offline payment receipts.
 */
object AccountManager {

    // TODO (Phase 7): Load from LocalCreditLedger and verify signed receipt
    private var _account: UserAccount = UserAccount(
        userId = "local-dev",
        username = "EcoMesh User",
        isMidTier = true,
        isPro = true,
    )

    fun getAccount(): UserAccount = _account

    /**
     * Called in Phase 7 once the payment receipt is verified offline.
     * Until then this is wired to a no-op stub.
     */
    fun upgradeTier(tier: AccountTier) {
        _account = _account.copy(
            isMidTier = tier >= AccountTier.MID,
            isPro = tier == AccountTier.PRO,
        )
    }
}

private operator fun AccountTier.compareTo(other: AccountTier): Int =
    this.ordinal.compareTo(other.ordinal)
