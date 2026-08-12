package com.pms.hotel.housekeeping.internal.web;

import java.time.Instant;

public record HousekeepingTaskView(
        Long id,
        Long roomId,
        String roomNumber,
        String taskType,
        String status,
        Long assignedTo,
        String notes,
        Instant createdAt) {
}
