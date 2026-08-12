package com.pms.hotel.reporting.internal;

import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.reporting.internal.ReportingViews.DashboardStats;
import com.pms.hotel.reporting.internal.ReportingViews.RevenueReport;
import com.pms.hotel.reporting.internal.ReportingViews.RevenueStats;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomOccupancyStats;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService {

    private final BookingApi bookingApi;
    private final RoomApi roomApi;

    public DashboardStats dashboardStats() {
        YearMonth month = YearMonth.now();
        var from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        var to = month.atEndOfMonth().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        BigDecimal monthlyRevenue = bookingApi.sumRevenueForStatusCreatedBetween("confirmed", from, to);

        RoomOccupancyStats occupancy = roomApi.occupancyStats();
        int occupancyRate = occupancy.totalRooms() > 0
                ? Math.round(100f * occupancy.occupiedRooms() / occupancy.totalRooms())
                : 0;

        long activeBookings = bookingApi.countByStatus("checked_in");
        BigDecimal activeRevenue = bookingApi.sumTotalAmountByStatus("checked_in");
        BigDecimal adr = activeBookings > 0
                ? activeRevenue.divide(BigDecimal.valueOf(activeBookings), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal revPar = adr.multiply(BigDecimal.valueOf(occupancyRate)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        return new DashboardStats(monthlyRevenue, occupancyRate, adr, revPar);
    }

    public RevenueReport revenueReport(LocalDate start, LocalDate end) {
        var data = bookingApi.revenueByCheckoutDateBetween(start, end);
        BigDecimal totalRevenue = data.stream().map(com.pms.hotel.booking.DailyRevenuePoint::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long daysCount = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start, end));
        long totalRooms = roomApi.occupancyStats().totalRooms();
        BigDecimal avgRevPar = totalRooms > 0
                ? totalRevenue.divide(BigDecimal.valueOf(daysCount * totalRooms), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new RevenueReport(data, new RevenueStats(totalRevenue, avgRevPar));
    }
}
