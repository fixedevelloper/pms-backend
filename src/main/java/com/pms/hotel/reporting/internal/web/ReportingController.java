package com.pms.hotel.reporting.internal.web;

import com.pms.hotel.reporting.internal.ReportingService;
import com.pms.hotel.reporting.internal.ReportingViews.DashboardStats;
import com.pms.hotel.reporting.internal.ReportingViews.RevenueReport;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class ReportingController {

    private final ReportingService reportingService;

    @GetMapping("/admin/dashboard-stats")
    public DashboardStats dashboardStats() {
        return reportingService.dashboardStats();
    }

    @GetMapping("/reports/revenue")
    public RevenueReport revenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(30);
        return reportingService.revenueReport(resolvedStart, resolvedEnd);
    }
}
