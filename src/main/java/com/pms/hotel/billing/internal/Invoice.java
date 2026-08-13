package com.pms.hotel.billing.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    public static final String UNPAID = "unpaid";
    public static final String PARTIALLY_PAID = "partially_paid";
    public static final String PAID = "paid";
    public static final String REFUNDED = "refunded";

    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "total_rooms", nullable = false)
    private BigDecimal totalRooms;

    @Column(name = "total_extras", nullable = false)
    private BigDecimal totalExtras;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "amount_paid", nullable = false)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "city_tax_amount", nullable = false)
    private BigDecimal cityTaxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private String status = UNPAID;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    /** Recomputes {@code amountPaid}/{@code status} from the completed payments recorded against this invoice. */
    public void refreshStatus(BigDecimal totalPaidFromCompletedPayments) {
        this.amountPaid = totalPaidFromCompletedPayments;
        if (totalPaidFromCompletedPayments.compareTo(totalAmount) >= 0) {
            this.status = PAID;
        } else if (totalPaidFromCompletedPayments.signum() > 0) {
            this.status = PARTIALLY_PAID;
        } else {
            this.status = UNPAID;
        }
    }
}
