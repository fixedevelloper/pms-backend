package com.pms.hotel.maintenance.internal.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public final class MaintenanceRequests {

    private MaintenanceRequests() {
    }

    public record CreateTicketRequest(
            @NotNull Long roomId,
            @NotBlank @Size(max = 255) String title,
            @Size(max = 2000) String description,
            @Pattern(regexp = "low|medium|high|urgent") String priority) {
    }

    /** Tous les champs optionnels — seuls ceux présents sont appliqués. */
    public record UpdateTicketRequest(
            @Size(max = 255) String title,
            @Size(max = 2000) String description,
            @Pattern(regexp = "low|medium|high|urgent") String priority,
            @Pattern(regexp = "open|in_progress|resolved|cancelled") String status,
            Long assignedTo,
            @DecimalMin("0") BigDecimal cost) {
    }
}
