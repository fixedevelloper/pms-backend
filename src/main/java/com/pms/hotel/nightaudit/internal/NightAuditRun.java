package com.pms.hotel.nightaudit.internal;

import com.pms.hotel.nightaudit.NightAuditRunResult;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "night_audit_runs")
public class NightAuditRun extends BaseEntity {

    @Column(name = "business_date", nullable = false, unique = true)
    private LocalDate businessDate;

    @Column(name = "ran_at", nullable = false)
    private Instant ranAt;

    @Column(name = "occupied_rooms", nullable = false)
    private int occupiedRooms;

    @Column(name = "total_revenue", nullable = false)
    private BigDecimal totalRevenue;

    @Column(name = "no_shows_processed", nullable = false)
    private int noShowsProcessed;

    @Column(name = "no_show_fees_total", nullable = false)
    private BigDecimal noShowFeesTotal;

    public NightAuditRunResult toResult() {
        return new NightAuditRunResult(businessDate, ranAt, occupiedRooms, totalRevenue, noShowsProcessed, noShowFeesTotal);
    }
}
