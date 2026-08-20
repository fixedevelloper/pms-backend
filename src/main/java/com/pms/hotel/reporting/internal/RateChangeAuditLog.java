package com.pms.hotel.reporting.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une ligne du journal d'audit des tarifs — persistée à chaque
 * RatePlanPriceChangedEvent (voir ReportingEventListener). Vit dans le
 * module reporting (et non rateplan) car c'est un journal de consultation
 * transverse, pas un état métier du module tarifaire lui-même.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rate_change_audit_logs")
public class RateChangeAuditLog extends BaseEntity {

    @Column(name = "rate_plan_id", nullable = false)
    private Long ratePlanId;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "new_price", nullable = false)
    private BigDecimal newPrice;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;
}
