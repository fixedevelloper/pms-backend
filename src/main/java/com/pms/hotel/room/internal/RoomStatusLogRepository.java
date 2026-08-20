package com.pms.hotel.room.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomStatusLogRepository extends JpaRepository<RoomStatusLog, Long> {

    List<RoomStatusLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
