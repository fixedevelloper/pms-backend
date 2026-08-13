package com.pms.hotel.housekeeping.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "housekeeping_tasks")
public class HousekeepingTask extends BaseEntity {

    public static final String PENDING = "pending";
    public static final String IN_PROGRESS = "in_progress";
    public static final String COMPLETED = "completed";

    public static final String LOW = "low";
    public static final String NORMAL = "normal";
    public static final String HIGH = "high";
    public static final String URGENT = "urgent";

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(nullable = false)
    private String priority = NORMAL;

    @Column(nullable = false)
    private String status = PENDING;

    @Column(name = "assigned_to")
    private Long assignedTo;

    private String notes;
}
