package com.pms.hotel.room.internal;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomBlockRepository extends JpaRepository<RoomBlock, Long> {

    @Query("""
            select case when count(rb) > 0 then true else false end
            from RoomBlock rb
            where rb.room.id = :roomId
                and rb.startDate < :checkOut
                and rb.endDate > :checkIn
            """)
    boolean existsOverlap(@Param("roomId") Long roomId, @Param("checkIn") LocalDate checkIn, @Param("checkOut") LocalDate checkOut);

    @Query("select distinct rb.room.id from RoomBlock rb where rb.startDate <= :date and rb.endDate > :date")
    List<Long> findBlockedRoomIds(@Param("date") LocalDate date);

    List<RoomBlock> findByRoomIdOrderByStartDateDesc(Long roomId);

    @Query("select rb from RoomBlock rb join rb.room r join r.roomType rt where rt.propertyId = :propertyId order by rb.startDate desc")
    List<RoomBlock> findByPropertyIdOrderByStartDateDesc(@Param("propertyId") Long propertyId);
}
