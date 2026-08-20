package com.pms.hotel.billing.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    public static final String PENDING = "pending";
    public static final String COMPLETED = "completed";
    public static final String FAILED = "failed";
    public static final String REFUNDED = "refunded";

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency = "XAF";

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private String status = PENDING;

    private String gateway;

    @Column(name = "transaction_reference", unique = true)
    private String transactionReference;

    @Column(name = "paid_at")
    private Instant paidAt;

    /**
     * Montant réellement remis par le client quand il paie dans une devise
     * différente de la devise de référence de l'établissement — {@code amount}/
     * {@code currency} restent l'équivalent en devise de référence (ce qui compte
     * pour le solde de la facture). Null pour un paiement dans la devise de
     * référence : pas de conversion à faire.
     */
    @Column(name = "tendered_amount")
    private BigDecimal tenderedAmount;

    @Column(name = "tendered_currency")
    private String tenderedCurrency;

    /** Taux appliqué au moment du paiement (tenderedCurrency -> devise de référence) — jamais recalculé rétroactivement, même si le taux configuré change ensuite. */
    @Column(name = "exchange_rate_used")
    private BigDecimal exchangeRateUsed;
}
