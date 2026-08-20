package com.pms.hotel.billing.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingId(Long bookingId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.invoiceId = :invoiceId and p.status = 'completed'")
    BigDecimal sumCompletedForInvoice(@Param("invoiceId") Long invoiceId);

    /** Paiements complétés d'un montant exact, dans la fenêtre de dates, pas déjà rapprochés d'une ligne de relevé — candidats au rapprochement bancaire automatique. */
    @Query("""
            select p from Payment p
            where p.status = 'completed'
                and p.amount = :amount
                and cast(p.paidAt as date) between :from and :to
                and p.id not in (select l.matchedPaymentId from BankStatementLine l where l.matchedPaymentId is not null)
            order by p.paidAt asc
            """)
    List<Payment> findUnreconciledCompletedMatchCandidates(
            @Param("amount") BigDecimal amount, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
