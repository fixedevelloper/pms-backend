package com.pms.hotel.booking.internal;

import com.pms.hotel.shared.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    /** Nullable: absent for pre-existing bookings and channel-manager upserts, which have no internal rate plan. */
    @Column(name = "rate_plan_id")
    private Long ratePlanId;

    @Column(name = "price_per_night", nullable = false)
    private BigDecimal pricePerNight;

    @Column(name = "adults_count", nullable = false)
    private Integer adultsCount = 1;

    @Column(name = "children_count", nullable = false)
    private Integer childrenCount = 0;

    @OneToMany(mappedBy = "bookingRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookingRoomOccupant> occupants = new LinkedHashSet<>();
}
