package com.pms.hotel.pos.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public final class PosRequests {

    private PosRequests() {
    }

    public record ChargeToRoomRequest(
            @NotNull Long bookingId,
            @NotBlank @Pattern(regexp = "restaurant|spa|bar|room_service|minibar") String department,
            @NotBlank String itemName,
            @NotNull @Min(1) Integer quantity,
            @NotNull @DecimalMin("0") BigDecimal unitPrice,
            String externalOrderId) {
    }
}
