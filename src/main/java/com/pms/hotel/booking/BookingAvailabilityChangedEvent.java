package com.pms.hotel.booking;

import java.time.LocalDate;

/** "booking_created" | "booking_cancelled" — publié par BookingService, une fois par chambre affectée. */
public record BookingAvailabilityChangedEvent(Long roomId, LocalDate checkIn, LocalDate checkOut, String action) {
}
