package com.pms.hotel.nightaudit.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Singleton (exactly one row in practice) — the hotel's business date, advanced only by a night audit run, never by the system clock alone. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "night_audit_state")
public class NightAuditState extends BaseEntity {

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;
}
