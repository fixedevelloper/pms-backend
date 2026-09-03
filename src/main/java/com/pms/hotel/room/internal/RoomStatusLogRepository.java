package com.pms.hotel.room.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomStatusLogRepository extends JpaRepository<RoomStatusLog, Long> {

    List<RoomStatusLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    @Query("""
            select l from RoomStatusLog l join l.room r join r.roomType rt
            where rt.propertyId = :propertyId and l.createdAt between :from and :to
            order by l.createdAt desc
            """)
    List<RoomStatusLog> findByPropertyIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            @Param("propertyId") Long propertyId, @Param("from") Instant from, @Param("to") Instant to);
}
