package com.pms.hotel.maintenance;

import java.math.BigDecimal;
import java.time.Instant;

/** "low"|"medium"|"high"|"urgent" priority ; "open"|"in_progress"|"resolved"|"cancelled" status. */
public record MaintenanceTicketView(
        Long id,
        Long roomId,
        String roomNumber,
        String title,
        String description,
        String priority,
        String status,
        Long assignedTo,
        Long reportedBy,
        BigDecimal cost,
        Instant resolvedAt,
        Instant createdAt) {
}
