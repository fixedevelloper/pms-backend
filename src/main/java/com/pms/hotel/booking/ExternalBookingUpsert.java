package com.pms.hotel.booking;

import java.math.BigDecimal;
import java.time.Instant;

/** Payload used by the channel-manager module to create/update a booking coming from an OTA. */
public record ExternalBookingUpsert(
        String externalReference,
        Long guestId,
        Instant checkedInAt,
        Instant checkedOutAt,
        String source,
        BigDecimal totalAmount,
        BigDecimal taxAmount,
        Long roomId,
        BigDecimal pricePerNight) {
}
