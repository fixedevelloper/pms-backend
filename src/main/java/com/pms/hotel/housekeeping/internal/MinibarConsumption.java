package com.pms.hotel.housekeeping.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Une consommation constatée en chambre lors du passage de l'étage. Facturée
 * immédiatement au séjour actif de la chambre si occupée (voir
 * HousekeepingService#recordMinibarConsumption) — jamais si la chambre est
 * vide au moment du contrôle (simple réassort, {@code billed} reste false).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "minibar_consumptions")
public class MinibarConsumption extends BaseEntity {

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "minibar_item_id", nullable = false)
    private MinibarItem minibarItem;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean billed = false;

    @Column(name = "recorded_by")
    private Long recordedBy;
}
