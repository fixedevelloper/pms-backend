package com.pms.hotel.guest;

import java.time.LocalDate;

/** Public, read-only view of a guest exposed to other modules. */
public record GuestSummary(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        String passportNumber,
        LocalDate dateOfBirth,
        String nationality,
        String address,
        /** "passport" | "national_id" | "driving_license" | "other". */
        String idDocumentType,
        String idDocumentNumber,
        LocalDate idDocumentExpiry,
        String preferredFloor,
        String preferredBedding,
        String allergies,
        boolean vip,
        String internalNotes,
        boolean marketingConsent,
        boolean blacklisted,
        String blacklistReason) {
}
