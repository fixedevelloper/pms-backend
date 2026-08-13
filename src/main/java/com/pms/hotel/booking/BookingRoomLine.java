package com.pms.hotel.booking;

import java.math.BigDecimal;
import java.util.List;

public record BookingRoomLine(
        Long roomId,
        String roomNumber,
        BigDecimal pricePerNight,
        /** Absent pour les réservations sans rate plan interne (préexistantes, channel manager). */
        Long ratePlanId,
        String ratePlanName,
        Integer adultsCount,
        Integer childrenCount,
        /** Accompagnants nommés, hors titulaire de la réservation (voir Booking.guestId). */
        List<RoomOccupant> occupants) {
}
