package com.pms.hotel.rateplan.internal.web;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public final class RatePlanRequests {

    private static final String CANCELLATION_POLICY_PATTERN = "flexible|non_refundable|partial_refund";

    private RatePlanRequests() {
    }

    public record CreateRatePlanRequest(
            @NotNull Long roomTypeId,
            @NotBlank String name,
            String description,
            @NotNull @DecimalMin("0") BigDecimal pricePerNight,
            Boolean breakfastIncluded,
            @NotBlank @Pattern(regexp = CANCELLATION_POLICY_PATTERN) String cancellationPolicy,
            @Min(0) Integer freeCancellationDays,
            @DecimalMin("0") @DecimalMax("100") BigDecimal cancellationFeePercent) {
    }

    /** Tous les champs optionnels — seuls ceux présents sont appliqués (comme UpdateRoomTypeRequest). */
    public record UpdateRatePlanRequest(
            String name,
            String description,
            @DecimalMin("0") BigDecimal pricePerNight,
            Boolean breakfastIncluded,
            @Pattern(regexp = CANCELLATION_POLICY_PATTERN) String cancellationPolicy,
            @Min(0) Integer freeCancellationDays,
            @DecimalMin("0") @DecimalMax("100") BigDecimal cancellationFeePercent,
            Boolean active) {
    }
}
