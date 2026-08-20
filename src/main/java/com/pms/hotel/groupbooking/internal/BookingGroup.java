package com.pms.hotel.groupbooking.internal;

import com.pms.hotel.groupbooking.GroupSummary;
import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un bloc de chambres réservé pour un événement (séminaire, mariage, agence de
 * voyage...) — les réservations individuelles de chaque participant
 * (Booking#groupId) sont créées normalement par la réception, juste
 * rattachées à ce groupe. Aucune réservation d'inventaire "molle" : les
 * allotements ({@link GroupRoomAllotment}) ne sont qu'indicatifs (suivi du
 * pick-up), ils ne bloquent aucune chambre tant qu'une réservation réelle
 * n'est pas créée — voir le plan Phase 3, limitation assumée.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "booking_groups")
public class BookingGroup extends BaseEntity {

    public static final String TENTATIVE = "tentative";
    public static final String CONFIRMED = "confirmed";
    public static final String CANCELLED = "cancelled";
    public static final String CLOSED = "closed";

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(nullable = false)
    private String name;

    /** Société facturée pour ce groupe, si applicable — indicatif seulement (pas de FK Java, voir company_id sur Booking). */
    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "check_in", nullable = false)
    private LocalDate checkIn;

    @Column(name = "check_out", nullable = false)
    private LocalDate checkOut;

    @Column(nullable = false)
    private String status = TENTATIVE;

    private String notes;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupRoomAllotment> allotments = new ArrayList<>();

    public GroupSummary toSummary() {
        return new GroupSummary(
                getId(), propertyId, name, companyId, contactName, contactEmail, contactPhone,
                checkIn, checkOut, status, notes,
                allotments.stream().map(GroupRoomAllotment::toView).toList());
    }
}
