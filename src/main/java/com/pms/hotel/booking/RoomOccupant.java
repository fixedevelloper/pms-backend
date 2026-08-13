package com.pms.hotel.booking;

/** Accompagnant nommé d'une chambre — voir BookingRoomOccupant côté interne. */
public record RoomOccupant(String firstName, String lastName, String passportNumber) {
}
