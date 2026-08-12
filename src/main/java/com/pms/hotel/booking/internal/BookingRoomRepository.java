package com.pms.hotel.booking.internal;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRoomRepository extends JpaRepository<BookingRoom, Long> {

    /** Standard hotel overlap algorithm: (StartA < EndB) AND (EndA > StartB), excluding cancelled bookings. */
    @Query("""
            select case when count(br) > 0 then true else false end
            from BookingRoom br
            where br.roomId = :roomId
                and br.booking.status <> 'cancelled'
                and br.booking.checkedInAt < :checkOut
                and br.booking.checkedOutAt > :checkIn
            """)
    boolean existsOverlap(@Param("roomId") Long roomId, @Param("checkIn") Instant checkIn, @Param("checkOut") Instant checkOut);

    @Query("""
            select br.booking from BookingRoom br
            where br.roomId = :roomId
                and br.booking.status = 'checked_in'
                and br.booking.checkedInAt <= :now
                and br.booking.checkedOutAt >= :now
            """)
    Optional<Booking> findActiveCheckedInBookingForRoom(@Param("roomId") Long roomId, @Param("now") Instant now);
}
