package com.pms.hotel.loyalty;

/**
 * @param pointsToNextTier null si déjà au palier le plus élevé (PLATINUM).
 * @param nextTier null si déjà au palier le plus élevé.
 */
public record LoyaltyAccountView(
        Long guestId,
        long totalPoints,
        LoyaltyTier tier,
        LoyaltyTier nextTier,
        Long pointsToNextTier) {
}
