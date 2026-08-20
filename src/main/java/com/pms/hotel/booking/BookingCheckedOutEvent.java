package com.pms.hotel.booking;

import java.math.BigDecimal;

/** Publié quand une réservation passe checked_out (voir BookingService#markCheckedOut) — déclenche notamment le gain de points de fidélité. */
public record BookingCheckedOutEvent(Long bookingId, Long guestId, BigDecimal totalAmount) {
}
