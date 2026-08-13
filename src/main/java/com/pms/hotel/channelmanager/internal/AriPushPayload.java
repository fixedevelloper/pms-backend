package com.pms.hotel.channelmanager.internal;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Payload générique Availability/Rate/Inventory poussé vers le webhook d'un canal. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AriPushPayload(
        String event,
        String roomExternalId,
        String roomNumber,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal pricePerNight,
        Instant timestamp) {
}
