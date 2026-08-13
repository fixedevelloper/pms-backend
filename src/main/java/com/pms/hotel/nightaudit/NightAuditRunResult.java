package com.pms.hotel.nightaudit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Snapshot of a single night audit closing — one per business date, never re-run for the same date. */
public record NightAuditRunResult(
        LocalDate businessDate,
        Instant ranAt,
        int occupiedRooms,
        BigDecimal totalRevenue,
        int noShowsProcessed,
        BigDecimal noShowFeesTotal) {
}
