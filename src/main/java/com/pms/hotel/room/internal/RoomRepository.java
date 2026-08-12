package com.pms.hotel.room.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    Optional<Room> findByExternalChannelRoomId(String externalChannelRoomId);

    boolean existsByRoomNumber(String roomNumber);

    long countByStatus(String status);
}
