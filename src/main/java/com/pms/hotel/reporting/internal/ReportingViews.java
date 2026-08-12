package com.pms.hotel.reporting.internal;

import com.pms.hotel.booking.DailyRevenuePoint;
import java.math.BigDecimal;
import java.util.List;

public final class ReportingViews {

    private ReportingViews() {
    }

    public record DashboardStats(
            BigDecimal monthlyRevenue,
            int occupancyRatePercent,
            BigDecimal adr,
            BigDecimal revPar) {
    }

    public record RevenueReport(List<DailyRevenuePoint> data, RevenueStats stats) {
    }

    public record RevenueStats(BigDecimal totalRevenue, BigDecimal avgRevPar) {
    }
}
