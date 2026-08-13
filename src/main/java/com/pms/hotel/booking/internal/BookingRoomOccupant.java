package com.pms.hotel.booking.internal;

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
 * Accompagnant nommé d'une chambre — distinct du titulaire de la réservation
 * (Booking.guestId). Exigé légalement à l'enregistrement dans beaucoup de
 * pays pour la police/l'immigration (voir V7__add_booking_room_occupants).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "booking_room_occupants")
public class BookingRoomOccupant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_room_id", nullable = false)
    private BookingRoom bookingRoom;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "passport_number")
    private String passportNumber;
}
