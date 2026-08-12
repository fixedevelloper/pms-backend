package com.pms.hotel.room.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public final class RoomRequests {

    private RoomRequests() {
    }

    public record CreateRoomTypeRequest(
            @NotBlank String name,
            String description,
            @NotNull @Min(1) Integer baseCapacity,
            @NotNull @DecimalMin("0") BigDecimal basePrice) {
    }

    public record CreateRoomRequest(
            @NotNull Long roomTypeId,
            @NotBlank String roomNumber,
            @NotNull Integer floor,
            @NotBlank @Pattern(regexp = "available|occupied|dirty|maintenance") String status) {
    }

    public record UpdateRoomStatusRequest(
            @NotBlank @Pattern(regexp = "available|occupied|dirty|maintenance") String status,
            String note) {
    }
}
