package com.pms.hotel.loyalty.internal;

import com.pms.hotel.loyalty.LoyaltyTransactionView;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ligne du grand livre de points — un client peut consulter d'où viennent ses points, un manager peut justifier un ajustement. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loyalty_transactions")
public class LoyaltyTransaction extends BaseEntity {

    public static final String EARN = "earn";
    public static final String REDEEM = "redeem";
    public static final String ADJUST = "adjust";

    @Column(name = "guest_id", nullable = false)
    private Long guestId;

    /** Null pour un ajustement manuel (pas lié à un séjour). */
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(nullable = false)
    private long points;

    @Column(nullable = false)
    private String type;

    private String description;

    public LoyaltyTransactionView toView() {
        return new LoyaltyTransactionView(getId(), guestId, bookingId, points, type, description, getCreatedAt());
    }
}
