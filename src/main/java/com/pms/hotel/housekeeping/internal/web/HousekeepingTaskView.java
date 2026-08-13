package com.pms.hotel.housekeeping.internal.web;

import java.time.Instant;

public record HousekeepingTaskView(
        Long id,
        Long roomId,
        String roomNumber,
        String taskType,
        String priority,
        String status,
        Long assignedTo,
        String notes,
        /** Départ prévu du séjour en cours dans la chambre, si occupée — null sinon (voir HousekeepingService#resolveCheckoutAt). */
        Instant roomCheckoutAt,
        Instant createdAt) {
}
