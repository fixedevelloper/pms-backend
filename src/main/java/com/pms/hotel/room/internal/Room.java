package com.pms.hotel.room.internal;

import com.pms.hotel.room.RoomDetails;
import com.pms.hotel.room.RoomStatuses;
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
@Table(name = "rooms")
public class Room extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(name = "room_number", nullable = false, unique = true)
    private String roomNumber;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false)
    private String status = RoomStatuses.AVAILABLE;

    @Column(name = "external_channel_room_id", unique = true)
    private String externalChannelRoomId;

    public RoomDetails toSummary() {
        return new RoomDetails(
                getId(),
                roomType.getPropertyId(),
                roomNumber,
                floor,
                status,
                roomType.getId(),
                roomType.getName(),
                roomType.getBasePrice(),
                externalChannelRoomId);
    }
}
