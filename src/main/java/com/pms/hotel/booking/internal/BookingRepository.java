package com.pms.hotel.booking.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByExternalReference(String externalReference);

    @Query("""
            select b from Booking b
            where (:status is null or b.status = :status)
                and (:guestId is null or b.guestId = :guestId)
            order by b.createdAt desc
            """)
    Page<Booking> search(@Param("status") String status, @Param("guestId") Long guestId, Pageable pageable);

    @Query("""
            select b from Booking b
            where cast(b.checkedInAt as date) = :date
                and b.status in ('pending', 'confirmed', 'checked_in')
            """)
    List<Booking> findArrivals(@Param("date") java.time.LocalDate date);

    @Query("""
            select b from Booking b
            where cast(b.checkedOutAt as date) = :date
                and b.status in ('confirmed', 'checked_in', 'checked_out')
            """)
    List<Booking> findDepartures(@Param("date") java.time.LocalDate date);

    /** Réservations dont la date d'arrivée prévue est passée sans check-in — candidates au no-show du night audit. */
    @Query("""
            select b from Booking b
            where b.status in ('pending', 'confirmed')
                and cast(b.checkedInAt as date) <= :date
            """)
    List<Booking> findNoShowCandidates(@Param("date") java.time.LocalDate date);

    @Query("""
            select sum(b.totalAmount) from Booking b
            where b.status = 'checked_out'
                and cast(b.checkedOutAt as date) = :date
            """)
    java.math.BigDecimal sumRevenueForCheckoutDate(@Param("date") java.time.LocalDate date);

    long countByStatus(String status);

    @Query("select coalesce(sum(b.totalAmount), 0) from Booking b where b.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@Param("status") String status);

    @Query("""
            select coalesce(sum(b.totalAmount), 0) from Booking b
            where b.status = :status and b.createdAt >= :from and b.createdAt < :to
            """)
    java.math.BigDecimal sumRevenueForStatusCreatedBetween(
            @Param("status") String status, @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select cast(b.checkedOutAt as date) as day, sum(b.totalAmount - b.discountAmount + b.taxAmount) as revenue
            from Booking b
            where b.status = 'checked_out'
                and cast(b.checkedOutAt as date) between :start and :end
            group by cast(b.checkedOutAt as date)
            order by cast(b.checkedOutAt as date) asc
            """)
    List<Object[]> revenueByCheckoutDateBetween(
            @Param("start") java.time.LocalDate start, @Param("end") java.time.LocalDate end);
}
