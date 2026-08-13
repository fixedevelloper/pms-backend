package com.pms.hotel.reporting.internal.web;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Room + its current checked-in stay (if any), for the housekeeping/rooms
 * screens — {@code RoomDetails} alone (GET /rooms) carries no booking
 * information. Lives in {@code reporting} (not {@code room}, where it was
 * first built) because composing room + booking + pos data can only be done
 * from a module that depends on all three without being depended on back —
 * {@code room} itself is a base module that {@code booking} and {@code pos}
 * both already depend on, so a {@code room -> booking}/{@code room -> pos}
 * edge here created a cycle ({@code ModularityTests} caught it).
 */
public record RoomOccupancyView(
        Long id,
        String roomNumber,
        int floor,
        String status,
        Long roomTypeId,
        String roomTypeName,
        BigDecimal basePricePerNight,
        String externalChannelRoomId,
        CurrentStayView currentBooking) {

    public record CurrentStayView(
            Long bookingId,
            Long guestId,
            String guestFullName,
            Instant checkOut,
            BigDecimal outstandingExtras) {
    }
}
