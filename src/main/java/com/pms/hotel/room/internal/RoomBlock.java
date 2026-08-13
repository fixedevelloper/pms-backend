package com.pms.hotel.room.internal;

import com.pms.hotel.room.RoomBlockView;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Blocage hors-vente d'une chambre (maintenance longue durée, rénovation,
 * usage interne) — distinct du statut opérationnel {@link Room#getStatus()}.
 * Demi-ouvert [startDate, endDate), même convention que checkIn/checkOut.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "room_blocks")
public class RoomBlock extends BaseEntity {

    public static final String MAINTENANCE = "maintenance";
    public static final String RENOVATION = "renovation";
    public static final String INTERNAL_USE = "internal_use";
    public static final String OTHER = "other";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String reason;

    private String notes;

    public RoomBlockView toView() {
        return new RoomBlockView(getId(), room.getId(), room.getRoomNumber(), startDate, endDate, reason, notes, getCreatedAt());
    }
}
