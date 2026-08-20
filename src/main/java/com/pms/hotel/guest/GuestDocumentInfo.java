package com.pms.hotel.guest;

import java.time.Instant;

/** Métadonnées d'un document d'identité déposé par le client — sans les octets (voir GuestApi#getDocumentContent pour le téléchargement). */
public record GuestDocumentInfo(Long id, Long guestId, String fileName, String contentType, Instant uploadedAt) {
}
