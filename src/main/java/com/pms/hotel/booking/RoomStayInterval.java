package com.pms.hotel.booking;

import java.time.Instant;

/** Une chambre occupée sur [checkIn, checkOut) par une réservation active — pour la prévision d'occupation. */
public record RoomStayInterval(Long roomId, Instant checkIn, Instant checkOut) {
}
