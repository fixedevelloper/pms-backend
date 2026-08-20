package com.pms.hotel.notification.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Default {@link SmsSender}: logs instead of actually sending, until a real provider is configured. */
@Component
@Slf4j
public class LoggingSmsSender implements SmsSender {

    @Override
    public void send(String toPhoneNumber, String message) {
        if (toPhoneNumber == null || toPhoneNumber.isBlank()) {
            log.warn("SMS non envoyé : aucun numéro de téléphone pour le destinataire. Message : {}", message);
            return;
        }
        log.info("[SMS -> {}] {}", toPhoneNumber, message);
    }
}
