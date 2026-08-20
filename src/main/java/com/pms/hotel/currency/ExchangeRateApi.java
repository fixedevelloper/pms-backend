package com.pms.hotel.currency;

import java.math.BigDecimal;

/**
 * Public entry point into the currency module for the rest of the
 * application. Other modules must depend only on this interface, never on
 * {@code com.pms.hotel.currency.internal} types.
 * <p>
 * Taux configurés manuellement dans Réglages (clé {@code exchange_rate_<FROM>_<TO>},
 * ex: {@code exchange_rate_EUR_XAF}) — pas d'appel à une API de change externe à
 * ce jour (aucun fournisseur/clé disponible à vérifier ; voir le plan Phase 4.2
 * pour un rafraîchissement automatique en évolution future).
 */
public interface ExchangeRateApi {

    /** 1 unité de {@code fromCurrency} = combien de {@code toCurrency}. Lève BusinessRuleException si aucun taux (ni son inverse) n'est configuré. */
    BigDecimal getRate(String fromCurrency, String toCurrency);

    BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency);
}
