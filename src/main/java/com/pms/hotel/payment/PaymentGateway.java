package com.pms.hotel.payment;

import java.math.BigDecimal;

/**
 * Point d'extension pour la capture d'un paiement en ligne (acompte du
 * Booking Engine public). Aucune implémentation par défaut n'est fournie —
 * tant qu'aucun bean {@code PaymentGateway} n'est enregistré, le moteur de
 * réservation public refuse la garantie "deposit" (voir
 * BookingPublicController) plutôt que de prétendre avoir encaissé un
 * paiement qui n'a jamais eu lieu.
 * <p>
 * Pour activer un vrai fournisseur (CinetPay, Stripe, Orange Money...),
 * ajouter un {@code @Component implements PaymentGateway} qui appelle son
 * SDK/API avec les identifiants du fournisseur.
 */
public interface PaymentGateway {

    PaymentChargeResult charge(BigDecimal amount, String currency, String paymentMethodToken, String description);
}
