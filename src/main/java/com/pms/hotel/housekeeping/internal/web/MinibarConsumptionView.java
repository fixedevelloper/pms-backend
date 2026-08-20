package com.pms.hotel.housekeeping.internal.web;

import java.math.BigDecimal;
import java.time.Instant;

public record MinibarConsumptionView(
        Long id,
        Long roomId,
        String roomNumber,
        Long minibarItemId,
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal totalPrice,
        boolean billed,
        Instant createdAt) {
}
