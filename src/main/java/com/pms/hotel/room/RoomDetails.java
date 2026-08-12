package com.pms.hotel.room;

import java.math.BigDecimal;

/** Public, read-only view of a room exposed to other modules. */
public record RoomDetails(
        Long id,
        String roomNumber,
        int floor,
        String status,
        Long roomTypeId,
        String roomTypeName,
        BigDecimal basePricePerNight,
        String externalChannelRoomId) {
}
