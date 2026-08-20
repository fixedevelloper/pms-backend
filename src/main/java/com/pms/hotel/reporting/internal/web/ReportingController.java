package com.pms.hotel.reporting.internal.web;

import com.pms.hotel.reporting.internal.ReportingService;
import com.pms.hotel.reporting.internal.ReportingViews.AuditTrailReport;
import com.pms.hotel.reporting.internal.ReportingViews.DashboardStats;
import com.pms.hotel.reporting.internal.ReportingViews.OccupancyForecastPoint;
import com.pms.hotel.reporting.internal.ReportingViews.PoliceRegisterEntry;
import com.pms.hotel.reporting.internal.ReportingViews.ProductionByChannelReport;
import com.pms.hotel.reporting.internal.ReportingViews.RevenueReport;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
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

    // URL inchangée (le frontend l'appelle déjà ainsi) — seul l'emplacement du
    // controller a bougé, de room vers reporting, pour casser un cycle de modules.
    @GetMapping("/rooms/occupancy")
    public List<RoomOccupancyView> roomOccupancy() {
        return reportingService.roomOccupancy();
    }

    @GetMapping("/reports/police-register")
    public List<PoliceRegisterEntry> policeRegister(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reportingService.policeRegister(date != null ? date : LocalDate.now());
    }

    @GetMapping("/reports/occupancy-forecast")
    public List<OccupancyForecastPoint> occupancyForecast(@RequestParam(defaultValue = "30") int days) {
        LocalDate today = LocalDate.now();
        return reportingService.occupancyForecast(today, today.plusDays(Math.max(1, days)));
    }

    @GetMapping("/reports/production-by-channel")
    public ProductionByChannelReport productionByChannel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(30);
        return reportingService.productionByChannel(resolvedStart, resolvedEnd);
    }

    @GetMapping("/reports/audit-trail")
    public AuditTrailReport auditTrail(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        LocalDate resolvedEnd = end != null ? end : LocalDate.now();
        LocalDate resolvedStart = start != null ? start : resolvedEnd.minusDays(30);
        Instant from = resolvedStart.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = resolvedEnd.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return reportingService.auditTrail(from, to);
    }
}
