package com.pms.hotel.pos;

import java.math.BigDecimal;

/**
 * Published whenever a restaurant/spa/bar charge is billed to a room.
 * The billing module listens for this to keep an already-generated invoice's
 * totals in sync, mirroring what the original Laravel controller did inline.
 */
public record ExtraChargeAddedEvent(Long bookingId, BigDecimal amount) {
}
