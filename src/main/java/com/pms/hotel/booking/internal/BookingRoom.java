package com.pms.hotel.booking.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Multi-room pivot for a booking. Rooms belong to the room module: this
 * entity only stores the room id (plus the nightly rate agreed at booking
 * time), never a JPA relationship into the room module's entities.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "booking_room")
public class BookingRoom extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;
}
