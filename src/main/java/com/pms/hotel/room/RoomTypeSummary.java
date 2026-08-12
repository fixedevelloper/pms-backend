package com.pms.hotel.room;

import java.math.BigDecimal;

public record RoomTypeSummary(
        Long id,
        String name,
        String description,
        int baseCapacity,
        BigDecimal basePrice) {
}
