package com.pms.hotel.billing.internal;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingId(Long bookingId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoiceId = :invoiceId and p.status = 'completed'")
    BigDecimal sumCompletedForInvoice(@Param("invoiceId") Long invoiceId);
}
