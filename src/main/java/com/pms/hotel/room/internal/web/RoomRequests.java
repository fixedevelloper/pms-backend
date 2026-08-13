package com.pms.hotel.room.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class RoomRequests {

    private RoomRequests() {
    }

    public record CreateRoomTypeRequest(
            @NotBlank String name,
            String description,
            @NotNull @Min(1) Integer baseCapacity,
            @NotNull @DecimalMin("0") BigDecimal basePrice) {
    }

    /** Tous les champs optionnels — seuls ceux présents sont appliqués (comme UpdateCategoryRequest). */
    public record UpdateRoomTypeRequest(
            String name,
            String description,
            @Min(1) Integer baseCapacity,
            @DecimalMin("0") BigDecimal basePrice) {
    }

    public record CreateRoomRequest(
            @NotNull Long roomTypeId,
            @NotBlank String roomNumber,
            @NotNull Integer floor,
            @NotBlank @Pattern(regexp = "available|occupied|dirty|maintenance") String status) {
    }

    public record UpdateRoomStatusRequest(
            @NotBlank @Pattern(regexp = "available|occupied|dirty|maintenance") String status,
            String note,
            /** Check-list qualité optionnelle (linge/salle de bain/minibar) — voir RoomStatusLog. */
            Boolean linenChecked,
            Boolean bathroomChecked,
            Boolean minibarChecked) {
    }

    public record CreateRoomBlockRequest(
            @NotNull @FutureOrPresent LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotBlank @Pattern(regexp = "maintenance|renovation|internal_use|other") String reason,
            @Size(max = 500) String notes) {
    }
}
