package com.pms.hotel.loyalty;

/**
 * Public entry point into the loyalty module for the rest of the
 * application. Other modules must depend only on this interface (and the
 * records it exposes), never on {@code com.pms.hotel.loyalty.internal} types.
 */
public interface LoyaltyApi {

    /** Crée le compte s'il n'existe pas encore (0 point, palier SILVER) — jamais de 404 pour un client qui n'a simplement encore rien accumulé. */
    LoyaltyAccountView getAccount(Long guestId);
}
