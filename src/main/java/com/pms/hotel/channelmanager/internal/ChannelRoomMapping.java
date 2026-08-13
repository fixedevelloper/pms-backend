package com.pms.hotel.channelmanager.internal;

import com.pms.hotel.channelmanager.ChannelRoomMappingView;
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

/**
 * Mapping chambre interne ↔ id de chambre externe, PAR CANAL — distinct de
 * rooms.external_channel_room_id (legacy, channel-agnostique, utilisé par le
 * seul webhook entrant existant).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "channel_room_mappings")
public class ChannelRoomMapping extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "channel_id", nullable = false)
    private Channel channel;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "external_room_id", nullable = false)
    private String externalRoomId;

    public ChannelRoomMappingView toView(String roomNumber) {
        return new ChannelRoomMappingView(getId(), channel.getId(), channel.getName(), roomId, roomNumber, externalRoomId);
    }
}
