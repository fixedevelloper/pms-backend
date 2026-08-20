package com.pms.hotel.housekeeping.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Article du catalogue minibar de l'établissement (remplace la case "minibar vérifié" globale sur RoomStatusLog). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "minibar_items")
public class MinibarItem extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private boolean active = true;
}
