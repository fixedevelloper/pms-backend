package com.pms.hotel.rateplan;

import java.math.BigDecimal;

/** Public, read-only view of a rate plan exposed to other modules (notably booking). */
public record RatePlanSummary(
        Long id,
        Long roomTypeId,
        String name,
        String description,
        BigDecimal pricePerNight,
        boolean breakfastIncluded,
        String cancellationPolicy,
        Integer freeCancellationDays,
        BigDecimal cancellationFeePercent,
        boolean active) {
}
