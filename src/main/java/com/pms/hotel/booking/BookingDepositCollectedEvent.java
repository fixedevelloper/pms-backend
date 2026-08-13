package com.pms.hotel.booking;

import java.math.BigDecimal;

/**
 * Published when a booking is created (or would in the future be updated)
 * with a non-zero upfront deposit, so the billing module can record it as a
 * completed payment — booking must not depend on billing directly (billing
 * already depends on booking via BookingApi; a direct call the other way
 * would create a module cycle, see ModularityTests).
 */
public record BookingDepositCollectedEvent(Long bookingId, BigDecimal amount) {
}
