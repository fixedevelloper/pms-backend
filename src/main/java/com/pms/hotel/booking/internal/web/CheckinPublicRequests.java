package com.pms.hotel.booking.internal.web;

import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public final class CheckinPublicRequests {

    private CheckinPublicRequests() {
    }

    public record CompleteCheckinRequest(
            LocalDate dateOfBirth,
            String nationality,
            String address,
            @Pattern(regexp = "passport|national_id|driving_license|other") String idDocumentType,
            String idDocumentNumber,
            LocalDate idDocumentExpiry) {
    }

    public record PublicCheckinView(
            Long bookingId,
            Instant checkIn,
            Instant checkOut,
            List<String> roomNumbers,
            String firstName,
            String lastName,
            String email,
            String phone,
            LocalDate dateOfBirth,
            String nationality,
            String address,
            String idDocumentType,
            String idDocumentNumber,
            LocalDate idDocumentExpiry,
            boolean completed) {
    }
}
