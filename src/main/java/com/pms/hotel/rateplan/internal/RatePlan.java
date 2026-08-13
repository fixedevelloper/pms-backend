package com.pms.hotel.rateplan.internal;

import com.pms.hotel.rateplan.RatePlanSummary;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rate_plans")
public class RatePlan extends BaseEntity {

    public static final String FLEXIBLE = "flexible";
    public static final String NON_REFUNDABLE = "non_refundable";
    public static final String PARTIAL_REFUND = "partial_refund";

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "breakfast_included", nullable = false)
    private boolean breakfastIncluded;

    @Column(name = "cancellation_policy", nullable = false)
    private String cancellationPolicy = FLEXIBLE;

    @Column(name = "free_cancellation_days")
    private Integer freeCancellationDays;

    @Column(name = "cancellation_fee_percent")
    private BigDecimal cancellationFeePercent;

    @Column(nullable = false)
    private boolean active = true;

    public RatePlanSummary toSummary() {
        return new RatePlanSummary(
                getId(), roomTypeId, name, description, pricePerNight, breakfastIncluded,
                cancellationPolicy, freeCancellationDays, cancellationFeePercent, active);
    }
}
