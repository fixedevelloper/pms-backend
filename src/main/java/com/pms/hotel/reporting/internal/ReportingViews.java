package com.pms.hotel.reporting.internal;

import com.pms.hotel.booking.DailyRevenuePoint;
import com.pms.hotel.booking.SourceRevenuePoint;
import java.math.BigDecimal;
import java.time.Instant;
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

    /** Rapport de production par canal/segment — quel intermédiaire (direct, OTA, etc.) génère le plus de valeur. */
    public record ProductionByChannelReport(List<SourceRevenuePoint> bySource, BigDecimal totalRevenue) {
    }

    /** Journal d'audit : modifications de tarifs et de statuts de chambres sur la période, pour prévenir la fraude interne. */
    public record AuditTrailReport(List<RateChangeEntry> rateChanges, List<RoomStatusChangeEntry> roomStatusChanges) {
    }

    public record RateChangeEntry(
            Long ratePlanId, String ratePlanName, Long roomTypeId, BigDecimal newPrice, Long changedByUserId, Instant changedAt) {
    }

    public record RoomStatusChangeEntry(
            Long roomId, String roomNumber, String status, String note, Long updatedByUserId, Instant changedAt) {
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
