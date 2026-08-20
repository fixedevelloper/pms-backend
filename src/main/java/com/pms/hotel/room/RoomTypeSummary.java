package com.pms.hotel.room;

import java.math.BigDecimal;

public record RoomTypeSummary(
        Long id,
        Long propertyId,
        String name,
        String description,
        int baseCapacity,
        BigDecimal basePrice) {
}
