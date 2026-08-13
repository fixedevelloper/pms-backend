package com.pms.hotel.channelmanager.internal;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Envoi sortant vers le webhook d'un canal (OTA/agrégateur). Best-effort : un
 * canal en échec (timeout, 4xx/5xx, DNS invalide) ne doit jamais faire
 * remonter d'exception vers l'appelant — c'est un événement métier interne
 * (réservation créée, tarif modifié...) qui déclenche l'envoi, pas une
 * requête utilisateur qui attend une réponse de l'OTA.
 */
@Component
class ChannelPushClient {

    private static final Logger log = LoggerFactory.getLogger(ChannelPushClient.class);

    private final RestClient restClient;

    ChannelPushClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    void push(Channel channel, Object payload) {
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(channel.getWebhookUrl())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON);
            if (channel.getWebhookSecret() != null && !channel.getWebhookSecret().isBlank()) {
                request = request.header("X-Webhook-Secret", channel.getWebhookSecret());
            }
            request.body(payload).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Échec de synchronisation vers le canal '{}' ({}) : {}", channel.getName(), channel.getWebhookUrl(), e.getMessage());
        }
    }
}
