package com.pms.hotel.booking;

/** A currently checked-in stay covering today, for a given room. Used by the point-of-sale module. */
public record ActiveStay(Long bookingId, Long guestId, String guestFullName) {
}
