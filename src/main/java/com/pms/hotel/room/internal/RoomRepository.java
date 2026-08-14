package com.pms.hotel.room.internal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    Optional<Room> findByExternalChannelRoomId(String externalChannelRoomId);

    boolean existsByRoomNumber(String roomNumber);

    long countByStatus(String status);

    long countByStatusAndIdNotIn(String status, Collection<Long> ids);

    /**
     * {@code roomType} is {@code FetchType.LAZY} (see {@link Room}) — RoomController#index maps
     * each row straight to {@code Room::toSummary} (which reads {@code roomType.getName()}/
     * {@code getBasePrice()}) outside any transaction, so the plain inherited {@code findAll(Sort)}
     * handed back an uninitialized proxy and blew up with LazyInitializationException the moment a
     * real room existed. This overrides it with an explicit fetch join instead.
     */
    @Query("select r from Room r join fetch r.roomType")
    List<Room> findAll(Sort sort);
}
