package com.pms.hotel.room.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "room_status_logs")
public class RoomStatusLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private String status;

    private String note;

    @Column(name = "updated_by")
    private Long updatedBy;

    /** Check-list qualité renseignée par le personnel d'étage lors du passage à "available" (voir HousekeepingRoomsGrid côté frontend). */
    @Column(name = "linen_checked", nullable = false)
    private boolean linenChecked = false;

    @Column(name = "bathroom_checked", nullable = false)
    private boolean bathroomChecked = false;

    @Column(name = "minibar_checked", nullable = false)
    private boolean minibarChecked = false;
}
