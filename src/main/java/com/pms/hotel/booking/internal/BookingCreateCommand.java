package com.pms.hotel.booking.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BookingCreateCommand(
        String firstName,
        String lastName,
        String email,
        String phone,
        String passportNumber,
        Instant checkIn,
        Instant checkOut,
        String source,
        List<Long> roomIds,
        BigDecimal totalAmount) {
}
