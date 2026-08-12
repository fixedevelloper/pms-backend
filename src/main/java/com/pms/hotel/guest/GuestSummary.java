package com.pms.hotel.guest;

/** Public, read-only view of a guest exposed to other modules. */
public record GuestSummary(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String passportNumber) {
}
