package com.pms.hotel.channelmanager.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChannelRoomMappingRepository extends JpaRepository<ChannelRoomMapping, Long> {

    List<ChannelRoomMapping> findByChannelId(Long channelId);

    /** Tous les canaux (actifs ou non — filtrés à l'usage) mappés pour une chambre donnée. */
    List<ChannelRoomMapping> findByRoomId(Long roomId);

    boolean existsByChannelIdAndRoomId(Long channelId, Long roomId);
}
