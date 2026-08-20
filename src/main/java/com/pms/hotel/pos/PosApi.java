package com.pms.hotel.pos;

import java.math.BigDecimal;
import java.util.List;

/**
 * Public entry point into the point-of-sale module for the rest of the
 * application. Other modules must depend only on this interface (and the
 * records it exposes), never on {@code com.pms.hotel.pos.internal} types.
 */
public interface PosApi {

    List<ExtraChargeLine> getChargesForBooking(Long bookingId);

    /** Sum of charges still owed by the room (statuses {@code charged_to_room} and {@code pending}). */
    BigDecimal getOutstandingTotalForBooking(Long bookingId);

    /** Bills a charge to a booking's folio — used by the housekeeping module to invoice minibar consumption. */
    ExtraChargeLine chargeToRoom(Long bookingId, String department, String itemName, int quantity, BigDecimal unitPrice, String externalOrderId);
}
