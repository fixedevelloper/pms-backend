package com.pms.hotel.loyalty.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class LoyaltyRequests {

    private LoyaltyRequests() {
    }

    public record AdjustPointsRequest(@NotNull Long points, @NotBlank String description) {
    }
}
