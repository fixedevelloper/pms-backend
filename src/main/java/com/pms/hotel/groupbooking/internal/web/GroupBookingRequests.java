package com.pms.hotel.groupbooking.internal.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public final class GroupBookingRequests {

    private GroupBookingRequests() {
    }

    public record CreateGroupRequest(
            @NotBlank String name,
            Long companyId,
            String contactName,
            @Email String contactEmail,
            String contactPhone,
            @NotNull LocalDate checkIn,
            @NotNull LocalDate checkOut,
            String notes) {
    }

    public record UpdateGroupRequest(
            String name,
            String contactName,
            @Email String contactEmail,
            String contactPhone,
            @Pattern(regexp = "tentative|confirmed|cancelled|closed") String status,
            String notes) {
    }

    public record CreateAllotmentRequest(
            @NotNull Long roomTypeId,
            Long ratePlanId,
            @NotNull @Min(1) Integer allottedRooms,
            String notes) {
    }

    public record UpdateAllotmentRequest(
            @Min(0) Integer allottedRooms,
            String notes) {
    }
}
