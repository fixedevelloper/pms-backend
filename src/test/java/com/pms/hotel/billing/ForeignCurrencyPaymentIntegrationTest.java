package com.pms.hotel.billing;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout en bout, contre une vraie base MySQL (Testcontainers) et le vrai
 * pipeline HTTP -> contrôleur -> service -> JPA : crée un type de chambre, une
 * chambre, un tarif, une réservation, génère la facture, configure un taux de
 * change, encaisse un paiement en devise étrangère, et vérifie la conversion
 * persistée ainsi que son effet sur le solde de la facture. Seule la
 * vérification de signature du JWT est doublée (SecurityMockMvcRequestPostProcessors) —
 * ce n'est pas ce que ce test cherche à couvrir.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
// spring-boot-starter-mail n'auto-configure un JavaMailSender que si un hôte est fourni
// (env var en déploiement réel) — sans lien avec ce que ce test vérifie, NotificationService
// avale déjà toute erreur d'envoi (voir son catch), seule l'existence du bean compte ici.
@TestPropertySource(properties = "spring.mail.host=localhost")
class ForeignCurrencyPaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    private static RequestPostProcessor admin() {
        return jwt()
                .jwt(builder -> builder.subject("990001").claim("name", "Test Admin"))
                .authorities(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_SUPER-ADMIN"),
                        new SimpleGrantedAuthority("manage rooms"),
                        new SimpleGrantedAuthority("manage housekeeping"),
                        new SimpleGrantedAuthority("settings.update"));
    }

    @Test
    void foreignCurrencyPaymentIsConvertedAtTheConfiguredRateAndSettlesTheInvoice() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());

        // 1) Type de chambre
        JsonNode roomType = postJson("/api/v1/room-types", """
                {"name": "Standard %s", "description": "Chambre standard", "base_capacity": 2, "base_price": 30000}
                """.formatted(suffix));
        long roomTypeId = roomType.get("id").asLong();

        // 2) Chambre physique
        JsonNode room = postJson("/api/v1/rooms", """
                {"room_type_id": %d, "room_number": "FX-%s", "floor": 1, "status": "available"}
                """.formatted(roomTypeId, suffix));
        long roomId = room.get("id").asLong();

        // 3) Tarif
        JsonNode ratePlan = postJson("/api/v1/rate-plans", """
                {"room_type_id": %d, "name": "Flexible", "price_per_night": 30000,
                 "breakfast_included": false, "cancellation_policy": "flexible"}
                """.formatted(roomTypeId));
        long ratePlanId = ratePlan.get("id").asLong();

        // 4) Réservation (aujourd'hui -> demain, une nuit, garantie "none")
        LocalDate checkIn = LocalDate.now();
        LocalDate checkOut = checkIn.plusDays(1);
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE;
        JsonNode booking = postJson("/api/v1/bookings", """
                {
                  "first_name": "Marie", "last_name": "Curie-%s",
                  "email": "marie.%s@example.test", "phone": "+237600000%s",
                  "check_in": "%s", "check_out": "%s",
                  "source": "direct", "guarantee_type": "none",
                  "rooms": [{"room_id": %d, "rate_plan_id": %d, "adults_count": 1, "children_count": 0}],
                  "total_amount": 30000
                }
                """.formatted(suffix, suffix, suffix.substring(suffix.length() - 3), iso.format(checkIn), iso.format(checkOut), roomId, ratePlanId));
        long bookingId = booking.get("id").asLong();
        assertThat(booking.get("status").asText()).isEqualTo("confirmed");

        // 5) Facture (marque aussi la réservation checked_out — voir BookingService#markCheckedOut)
        JsonNode invoice = postJson("/api/v1/bookings/" + bookingId + "/invoice/generate", null);
        assertThat(invoice.get("total_amount").decimalValue()).isEqualByComparingTo("30000");
        assertThat(invoice.get("status").asText()).isEqualTo("unpaid");

        // 6) Taux de change configuré manuellement (Réglages > Devises)
        BigDecimal rate = new BigDecimal("655.957");
        mockMvc.perform(post("/api/v1/settings")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"settings\": {\"exchange_rate_EUR_XAF\": \"" + rate + "\"}}"))
                .andExpect(status().isOk());

        // 7) Encaissement en devise étrangère
        BigDecimal tendered = new BigDecimal("45.75");
        BigDecimal expectedReferenceAmount = tendered.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        JsonNode payment = postJson("/api/v1/bookings/" + bookingId + "/payments/foreign-currency", """
                {"tendered_amount": %s, "tendered_currency": "eur", "payment_method": "cash"}
                """.formatted(tendered));

        assertThat(payment.get("currency").asText()).isEqualTo("XAF");
        assertThat(payment.get("amount").decimalValue()).isEqualByComparingTo(expectedReferenceAmount);
        assertThat(payment.get("tendered_amount").decimalValue()).isEqualByComparingTo(tendered);
        assertThat(payment.get("tendered_currency").asText()).isEqualTo("EUR");
        assertThat(payment.get("exchange_rate_used").decimalValue()).isEqualByComparingTo(rate);
        assertThat(payment.get("status").asText()).isEqualTo("completed");

        // 8) Le solde de la facture reflète le montant converti (30 010,03 > 30 000 -> soldée)
        String billingResponse = mockMvc.perform(get("/api/v1/bookings/" + bookingId + "/billing").with(admin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode billingJson = jsonMapper.readTree(billingResponse);

        assertThat(billingJson.get("invoice").get("amount_paid").decimalValue()).isEqualByComparingTo(expectedReferenceAmount);
        assertThat(billingJson.get("invoice").get("status").asText()).isEqualTo("paid");
        assertThat(billingJson.get("payments")).hasSize(1);
        JsonNode paymentInLedger = billingJson.get("payments").get(0);
        assertThat(paymentInLedger.get("tendered_currency").asText()).isEqualTo("EUR");
        assertThat(paymentInLedger.get("exchange_rate_used").decimalValue()).isEqualByComparingTo(rate);
    }

    private JsonNode postJson(String url, String body) throws Exception {
        var request = post(url).with(admin()).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request = request.content(body);
        } else {
            request = request.content("");
        }
        String response = mockMvc.perform(request)
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        return jsonMapper.readTree(response);
    }
}
