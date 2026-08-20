package com.pms.hotel.loyalty;

import java.time.Instant;

public record LoyaltyTransactionView(
        Long id,
        Long guestId,
        Long bookingId,
        long points,
        String type,
        String description,
        Instant createdAt) {
}
