package com.pms.hotel.guest;

/** Data needed to create or update a guest, as collected at booking/channel-manager time. */
public record GuestUpsertRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String passportNumber) {
}
