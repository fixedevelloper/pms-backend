package com.pms.hotel.maintenance.internal;

import com.pms.hotel.maintenance.MaintenanceTicketView;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "maintenance_tickets")
public class MaintenanceTicket extends BaseEntity {

    public static final String LOW = "low";
    public static final String MEDIUM = "medium";
    public static final String HIGH = "high";
    public static final String URGENT = "urgent";

    public static final String OPEN = "open";
    public static final String IN_PROGRESS = "in_progress";
    public static final String RESOLVED = "resolved";
    public static final String CANCELLED = "cancelled";

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String priority = MEDIUM;

    @Column(nullable = false)
    private String status = OPEN;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "reported_by")
    private Long reportedBy;

    private BigDecimal cost;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public MaintenanceTicketView toView(String roomNumber) {
        return new MaintenanceTicketView(
                getId(), roomId, roomNumber, title, description, priority, status,
                assignedTo, reportedBy, cost, resolvedAt, getCreatedAt());
    }
}
