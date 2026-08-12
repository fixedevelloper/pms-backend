package com.pms.hotel.pos;

import java.math.BigDecimal;

public record ExtraChargeLine(
        Long id,
        Long bookingId,
        String department,
        String itemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String paymentStatus) {
}
