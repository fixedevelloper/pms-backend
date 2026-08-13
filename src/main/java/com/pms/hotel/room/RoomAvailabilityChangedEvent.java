package com.pms.hotel.room;

import java.time.LocalDate;

/** "blocked" | "unblocked" — publié par RoomService sur création/levée d'un blocage hors-vente. */
public record RoomAvailabilityChangedEvent(Long roomId, LocalDate startDate, LocalDate endDate, String action) {
}
