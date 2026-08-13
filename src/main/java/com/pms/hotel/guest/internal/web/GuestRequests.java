package com.pms.hotel.guest.internal.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class GuestRequests {

    private GuestRequests() {
    }

    private static final String ID_DOCUMENT_TYPE_PATTERN = "passport|national_id|driving_license|other";

    public record CreateGuestRequest(
            @NotBlank @Size(max = 255) String firstName,
            @NotBlank @Size(max = 255) String lastName,
            @NotBlank @Email String email,
            @Size(max = 20) String phone,
            @Size(max = 50) String passportNumber,
            LocalDate dateOfBirth,
            @Size(max = 100) String nationality,
            @Size(max = 500) String address,
            @Pattern(regexp = ID_DOCUMENT_TYPE_PATTERN) String idDocumentType,
            @Size(max = 100) String idDocumentNumber,
            LocalDate idDocumentExpiry,
            @Size(max = 50) String preferredFloor,
            @Size(max = 100) String preferredBedding,
            @Size(max = 500) String allergies,
            boolean vip,
            String internalNotes,
            boolean marketingConsent,
            boolean blacklisted,
            @Size(max = 500) String blacklistReason) {
    }

    public record UpdateGuestRequest(
            @Size(max = 255) String firstName,
            @Size(max = 255) String lastName,
            @Email String email,
            @Size(max = 20) String phone,
            @Size(max = 50) String passportNumber,
            LocalDate dateOfBirth,
            @Size(max = 100) String nationality,
            @Size(max = 500) String address,
            @Pattern(regexp = ID_DOCUMENT_TYPE_PATTERN) String idDocumentType,
            @Size(max = 100) String idDocumentNumber,
            LocalDate idDocumentExpiry,
            @Size(max = 50) String preferredFloor,
            @Size(max = 100) String preferredBedding,
            @Size(max = 500) String allergies,
            Boolean vip,
            String internalNotes,
            Boolean marketingConsent,
            Boolean blacklisted,
            @Size(max = 500) String blacklistReason) {
    }
}
