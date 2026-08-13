package com.pms.hotel.reporting.internal;

import com.pms.hotel.booking.DailyRevenuePoint;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    public record OccupancyForecastPoint(LocalDate date, long occupiedRooms, long totalRooms, int occupancyRatePercent) {
    }

    /**
     * Une ligne du registre d'arrivées — le titulaire de la réservation (profil
     * complet) et chaque accompagnant nommé (nom/passeport seulement, faute de
     * profil complet pour eux — voir com.pms.hotel.booking.RoomOccupant).
     */
    public record PoliceRegisterEntry(
            Long bookingId,
            String roomNumber,
            String lastName,
            String firstName,
            boolean primaryGuest,
            LocalDate dateOfBirth,
            String nationality,
            String idDocumentType,
            String idDocumentNumber,
            LocalDate arrivalDate,
            LocalDate departureDate) {
    }
}
