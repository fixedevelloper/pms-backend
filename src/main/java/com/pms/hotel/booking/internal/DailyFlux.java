package com.pms.hotel.booking.internal;

import java.math.BigDecimal;
import java.util.List;

public record DailyFlux(List<StayFlux> arrivals, List<StayFlux> departures, Stats stats) {

    public record StayFlux(Long id, String guestName, String roomNumbers, String status, boolean onlineCheckinCompleted) {
    }

    public record Stats(int occupancyRate, long cleanRooms, long totalRooms, BigDecimal dailyRevenue) {
    }
}
