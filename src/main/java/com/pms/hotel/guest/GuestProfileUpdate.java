package com.pms.hotel.guest;

import java.time.LocalDate;

/** Champs modifiables par le client lui-même lors du pré-enregistrement en ligne (voir booking.internal.web.CheckinPublicController). */
public record GuestProfileUpdate(
        LocalDate dateOfBirth,
        String nationality,
        String address,
        String idDocumentType,
        String idDocumentNumber,
        LocalDate idDocumentExpiry) {
}
