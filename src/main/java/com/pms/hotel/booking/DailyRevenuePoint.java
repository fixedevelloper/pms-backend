package com.pms.hotel.booking;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenuePoint(LocalDate date, BigDecimal revenue) {
}
