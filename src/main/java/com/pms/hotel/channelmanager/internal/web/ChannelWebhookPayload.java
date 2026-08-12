package com.pms.hotel.channelmanager.internal.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ChannelWebhookPayload(String action, Data data) {

    public record Data(
            String id,
            Customer customer,
            @JsonProperty("arrival_date") LocalDate arrivalDate,
            @JsonProperty("departure_date") LocalDate departureDate,
            @JsonProperty("ota_name") String otaName,
            BigDecimal amount,
            @JsonProperty("tax_amount") BigDecimal taxAmount,
            @JsonProperty("channel_room_id") String channelRoomId,
            @JsonProperty("amount_per_night") BigDecimal amountPerNight) {
    }

    public record Customer(
            String email,
            @JsonProperty("first_name") String firstName,
            @JsonProperty("last_name") String lastName,
            String phone) {
    }
}
