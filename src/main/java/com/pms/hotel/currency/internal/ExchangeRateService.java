package com.pms.hotel.currency.internal;

import com.pms.hotel.currency.ExchangeRateApi;
import com.pms.hotel.settings.SettingsApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExchangeRateService implements ExchangeRateApi {

    private final SettingsApi settingsApi;

    @Override
    public BigDecimal getRate(String fromCurrency, String toCurrency) {
        String from = fromCurrency.toUpperCase(Locale.ROOT);
        String to = toCurrency.toUpperCase(Locale.ROOT);
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        var direct = settingsApi.get(settingKey(from, to));
        if (direct.isPresent() && !direct.get().isBlank()) {
            return new BigDecimal(direct.get());
        }

        // Le taux inverse a pu être configuré à la place (ex: XAF -> EUR au lieu de EUR -> XAF) — on l'inverse plutôt que d'exiger les deux sens.
        var inverse = settingsApi.get(settingKey(to, from));
        if (inverse.isPresent() && !inverse.get().isBlank()) {
            BigDecimal inverseRate = new BigDecimal(inverse.get());
            if (inverseRate.signum() == 0) {
                throw new BusinessRuleException("Taux de change invalide (zéro) pour " + to + " → " + from + ".");
            }
            return BigDecimal.ONE.divide(inverseRate, 8, RoundingMode.HALF_UP);
        }

        throw new BusinessRuleException(
                "Aucun taux de change configuré pour " + from + " → " + to + ". Configurez-le dans Réglages > Devises.");
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        return amount.multiply(getRate(fromCurrency, toCurrency)).setScale(2, RoundingMode.HALF_UP);
    }

    private String settingKey(String from, String to) {
        return "exchange_rate_" + from + "_" + to;
    }
}
