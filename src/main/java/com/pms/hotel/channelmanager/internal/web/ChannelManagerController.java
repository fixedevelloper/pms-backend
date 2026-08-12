package com.pms.hotel.channelmanager.internal.web;

import com.pms.hotel.channelmanager.internal.ChannelManagerService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public webhook endpoint the channel manager (OTA aggregator) calls into.
 * Authenticated via a shared secret header, not a JWT: this caller is another
 * system, not one of our users. See {@code SecurityConfig} for the matching
 * {@code permitAll()} rule.
 */
@RestController
@RequestMapping("/api/v1/channel-manager")
@RequiredArgsConstructor
class ChannelManagerController {

    private static final String TOKEN_HEADER = "X-Channex-Webhook-Token";

    private final ChannelManagerService channelManagerService;

    @Value("${pms.channel-manager.webhook-token}")
    private String expectedToken;

    @PostMapping("/bookings")
    public ResponseEntity<?> handle(HttpServletRequest request, @RequestBody ChannelWebhookPayload payload) {
        String providedToken = request.getHeader(TOKEN_HEADER);
        if (!expectedToken.equals(providedToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("error", "Unauthorized"));
        }

        channelManagerService.handle(payload);
        return ResponseEntity.ok(java.util.Map.of("status", "acknowledged"));
    }
}
