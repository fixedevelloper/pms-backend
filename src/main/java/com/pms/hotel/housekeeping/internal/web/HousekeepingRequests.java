package com.pms.hotel.housekeeping.internal.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class HousekeepingRequests {

    private HousekeepingRequests() {
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
