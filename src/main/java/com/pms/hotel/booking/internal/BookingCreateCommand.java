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
        String guaranteeType,
        BigDecimal depositAmount,
        List<RoomAllocation> rooms,
        BigDecimal totalAmount) {

    public record RoomAllocation(
            Long roomId, Long ratePlanId, Integer adultsCount, Integer childrenCount, List<OccupantInput> occupants) {
    }

    public record OccupantInput(String firstName, String lastName, String passportNumber) {
    }
}
