package com.pms.hotel.pos.internal;

import com.pms.hotel.pos.ExtraChargeLine;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "extra_charges")
public class ExtraCharge extends BaseEntity {

    public static final String CHARGED_TO_ROOM = "charged_to_room";
    public static final String PENDING = "pending";
    public static final String PAID_INSTANTLY = "paid_instantly";

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(nullable = false)
    private String department;

    @Column(name = "item_name", nullable = false)
    private String itemName;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = CHARGED_TO_ROOM;

    /** Mirrors the Laravel model's {@code saving} hook: total = unit_price * quantity + tax - discount. */
    @PrePersist
    @PreUpdate
    void computeTotalPrice() {
        BigDecimal base = unitPrice.multiply(BigDecimal.valueOf(quantity));
        totalPrice = base.add(taxAmount).subtract(discountAmount);
    }

    public ExtraChargeLine toLine() {
        return new ExtraChargeLine(getId(), bookingId, department, itemName, quantity, unitPrice, totalPrice, paymentStatus);
    }
}
