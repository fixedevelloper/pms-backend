package com.pms.hotel.housekeeping.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public final class HousekeepingRequests {

    private HousekeepingRequests() {
    }

    public record CreateMinibarItemRequest(
            @NotBlank String name,
            @NotNull @DecimalMin("0") BigDecimal unitPrice) {
    }

    public record RecordMinibarConsumptionRequest(
            @NotNull Long minibarItemId,
            @NotNull @Min(1) Integer quantity) {
    }

    public record ReportLostFoundItemRequest(
            Long roomId,
            @NotBlank String description,
            String foundLocation,
            String notes) {
    }

    public record UpdateLostFoundStatusRequest(
            @NotBlank @Pattern(regexp = "stored|claimed|disposed") String status,
            String claimantName) {
    }

    public record CreateTaskRequest(
            @NotNull Long roomId,
            @NotBlank @Pattern(regexp = "cleaning|laundry|maintenance|inspection") String taskType,
            @Pattern(regexp = "low|normal|high|urgent") String priority,
            Long assignedTo,
            String notes) {
    }

    public record UpdateTaskStatusRequest(
            @NotBlank @Pattern(regexp = "pending|in_progress|completed") String status) {
    }

    public record UpdateTaskPriorityRequest(
            @NotBlank @Pattern(regexp = "low|normal|high|urgent") String priority) {
    }
}
