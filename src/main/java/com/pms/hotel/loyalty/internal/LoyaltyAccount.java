package com.pms.hotel.loyalty.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount extends BaseEntity {

    @Column(name = "guest_id", nullable = false, unique = true)
    private Long guestId;

    @Column(name = "total_points", nullable = false)
    private long totalPoints = 0;
}
