package com.pms.hotel.shared.security;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Vérifie un jeton Google reCAPTCHA v3 côté serveur. Désactivé tant
 * qu'aucune clé secrète n'est configurée ({@code pms.public-booking.recaptcha-secret})
 * — {@link #verify} accepte alors tout jeton (y compris absent), pour que le
 * Booking Engine public reste fonctionnel avant que le CAPTCHA ne soit
 * activé.
 */
@Component
@Slf4j
public class RecaptchaVerifier {

    private final RestClient restClient = RestClient.create();

    @Value("${pms.public-booking.recaptcha-secret:}")
    private String secretKey;

    public boolean isEnabled() {
        return secretKey != null && !secretKey.isBlank();
    }

    public boolean verify(String token) {
        if (!isEnabled()) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Map<?, ?> result = restClient.post()
                    .uri("https://www.google.com/recaptcha/api/siteverify?secret={secret}&response={token}", secretKey, token)
                    .retrieve()
                    .body(Map.class);
            return result != null && Boolean.TRUE.equals(result.get("success"));
        } catch (Exception e) {
            log.warn("Échec de la vérification reCAPTCHA", e);
            return false;
        }
    }
}
