package com.pms.hotel.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingSummary(
        Long id,
        Long guestId,
        String status,
        String source,
        String guaranteeType,
        BigDecimal depositAmount,
        /** Non-null seulement après une annulation effective — voir BookingService#computeCancellationFee. */
        BigDecimal cancellationFeeAmount,
        String externalReference,
        Instant checkedInAt,
        Instant checkedOutAt,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        List<BookingRoomLine> rooms) {
}
