package com.pms.hotel.room;

import java.time.Instant;
import java.time.LocalDate;

/** "maintenance" | "renovation" | "internal_use" | "other". */
public record RoomBlockView(
        Long id,
        Long roomId,
        String roomNumber,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String notes,
        Instant createdAt) {
}
