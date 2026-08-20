package com.pms.hotel.groupbooking.internal;

import com.pms.hotel.groupbooking.GroupRoomAllotmentView;
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

/** Nombre de chambres promises/tenues pour un type de chambre donné — indicatif (voir BookingGroup, pas un blocage d'inventaire). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "group_room_allotments")
public class GroupRoomAllotment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private BookingGroup group;

    @Column(name = "room_type_id", nullable = false)
    private Long roomTypeId;

    @Column(name = "rate_plan_id")
    private Long ratePlanId;

    @Column(name = "allotted_rooms", nullable = false)
    private Integer allottedRooms;

    private String notes;

    public GroupRoomAllotmentView toView() {
        return new GroupRoomAllotmentView(getId(), roomTypeId, ratePlanId, allottedRooms, notes);
    }
}
