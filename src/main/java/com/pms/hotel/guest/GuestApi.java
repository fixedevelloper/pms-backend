package com.pms.hotel.guest;

/**
 * Public entry point into the guest module for the rest of the application.
 * Other modules must depend only on this interface (and the records it exposes),
 * never on {@code com.pms.hotel.guest.internal} types.
 */
public interface GuestApi {

    GuestSummary getById(Long id);

    /** Finds the guest by phone number, creating (or refreshing) it otherwise. Used by the booking module. */
    GuestSummary findOrCreateByPhone(GuestUpsertRequest request);

    /** Finds the guest by email, creating (or refreshing) it otherwise. Used by the channel-manager module. */
    GuestSummary findOrCreateByEmail(GuestUpsertRequest request);

    /** Applique les champs non-null de {@code update} — utilisé par le pré-enregistrement en ligne (voir booking.internal.web.CheckinPublicController). */
    GuestSummary updateProfile(Long guestId, GuestProfileUpdate update);

    /** Dépose une pièce d'identité pour ce client — utilisé par le pré-enregistrement en ligne. */
    GuestDocumentInfo attachDocument(Long guestId, String fileName, String contentType, byte[] data);
}
