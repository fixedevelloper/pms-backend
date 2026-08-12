package com.pms.hotel.pos.internal;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtraChargeRepository extends JpaRepository<ExtraCharge, Long> {

    List<ExtraCharge> findByBookingId(Long bookingId);

    @Query("""
            select coalesce(sum(e.totalPrice), 0) from ExtraCharge e
            where e.bookingId = :bookingId and e.paymentStatus in ('charged_to_room', 'pending')
            """)
    BigDecimal sumOutstandingForBooking(@Param("bookingId") Long bookingId);
}
