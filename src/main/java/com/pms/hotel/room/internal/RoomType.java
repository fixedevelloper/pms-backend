package com.pms.hotel.room.internal;

import com.pms.hotel.room.RoomTypeSummary;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "room_types")
public class RoomType extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "base_capacity", nullable = false)
    private Integer baseCapacity;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    public RoomTypeSummary toSummary() {
        return new RoomTypeSummary(getId(), name, description, baseCapacity, basePrice);
    }
}
