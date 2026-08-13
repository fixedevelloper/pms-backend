package com.pms.hotel.booking.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bookings")
public class Booking extends BaseEntity {

    public static final String PENDING = "pending";
    public static final String CONFIRMED = "confirmed";
    public static final String CHECKED_IN = "checked_in";
    public static final String CHECKED_OUT = "checked_out";
    public static final String CANCELLED = "cancelled";
    /** Jamais posé manuellement par le staff — uniquement par NightAuditService lors d'une clôture. */
    public static final String NO_SHOW = "no_show";

    @Column(name = "guest_id", nullable = false)
    private Long guestId;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_out_at")
    private Instant checkedOutAt;

    @Column(nullable = false)
    private String status = PENDING;

    @Column(nullable = false)
    private String source = "direct";

    public static final String GUARANTEE_NONE = "none";
    public static final String GUARANTEE_CREDIT_CARD = "credit_card";
    public static final String GUARANTEE_DEPOSIT = "deposit";
    public static final String GUARANTEE_COMPANY = "company";

    @Column(name = "guarantee_type", nullable = false)
    private String guaranteeType = GUARANTEE_NONE;

    @Column(name = "deposit_amount", nullable = false)
    private BigDecimal depositAmount = BigDecimal.ZERO;

    /** Null tant que la réservation n'a jamais été annulée — calculé une seule fois lors de l'annulation. */
    @Column(name = "cancellation_fee_amount")
    private BigDecimal cancellationFeeAmount;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingRoom> rooms = new ArrayList<>();
}
