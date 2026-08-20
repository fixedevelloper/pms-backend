package com.pms.hotel.booking;

import java.math.BigDecimal;

/** Chiffre d'affaires et volume de réservations sorties (checked_out) pour un canal/segment donné (Booking#source). */
public record SourceRevenuePoint(String source, long bookingsCount, BigDecimal revenue) {
}
