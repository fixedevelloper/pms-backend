package com.pms.hotel.booking;

import java.math.BigDecimal;

public record BookingRoomLine(Long roomId, String roomNumber, BigDecimal pricePerNight) {
}
