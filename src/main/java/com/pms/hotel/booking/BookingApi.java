package com.pms.hotel.booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Public entry point into the booking module for the rest of the application.
 * Other modules must depend only on this interface (and the records it
 * exposes), never on {@code com.pms.hotel.booking.internal} types.
 */
public interface BookingApi {

    BookingSummary getById(Long bookingId);

    /** The currently checked-in stay covering today for a room, if any. Used by the point-of-sale module. */
    Optional<ActiveStay> findActiveCheckedInStay(Long roomId);

    /** Creates or updates a booking coming from the channel manager, matched by its external reference. */
    BookingSummary upsertFromExternalChannel(ExternalBookingUpsert command);

    void cancelByExternalReference(String externalReference);

    /** Marks a booking as checked-out, as done automatically when its final invoice is generated. */
    void markCheckedOut(Long bookingId);

    // --- Reporting queries -------------------------------------------------

    long countByStatus(String status);

    /** Comme {@link #countByStatus(String)}, restreint à un établissement — utilisé par le reporting property-scopé. */
    long countByStatus(Long propertyId, String status);

    BigDecimal sumTotalAmountByStatus(String status);

    /** Comme {@link #sumTotalAmountByStatus(String)}, restreint à un établissement. */
    BigDecimal sumTotalAmountByStatus(Long propertyId, String status);

    BigDecimal sumRevenueForStatusCreatedBetween(String status, Instant from, Instant to);

    /** Comme {@link #sumRevenueForStatusCreatedBetween(String, Instant, Instant)}, restreint à un établissement. */
    BigDecimal sumRevenueForStatusCreatedBetween(Long propertyId, String status, Instant from, Instant to);

    List<DailyRevenuePoint> revenueByCheckoutDateBetween(LocalDate start, LocalDate end);

    /** Comme {@link #revenueByCheckoutDateBetween(LocalDate, LocalDate)}, restreint à un établissement. */
    List<DailyRevenuePoint> revenueByCheckoutDateBetween(Long propertyId, LocalDate start, LocalDate end);

    /** Réservations dont l'arrivée (checkedInAt) tombe le {@code date} donné — utilisé par le registre de police/immigration. */
    List<BookingSummary> findArrivalsOn(LocalDate date);

    /** Comme {@link #findArrivalsOn(LocalDate)}, restreint à un établissement. */
    List<BookingSummary> findArrivalsOn(Long propertyId, LocalDate date);

    /** Séjours actifs (hors annulés/no-show) chevauchant [from, to) — utilisé par la prévision d'occupation. */
    List<RoomStayInterval> findRoomStaysOverlapping(LocalDate from, LocalDate to);

    /** Comme {@link #findRoomStaysOverlapping(LocalDate, LocalDate)}, restreint à un établissement. */
    List<RoomStayInterval> findRoomStaysOverlapping(Long propertyId, LocalDate from, LocalDate to);

    /** Réservations attendues le {@code onOrBeforeDate} (ou avant) qui n'ont jamais été enregistrées — utilisé par le night audit. */
    List<BookingSummary> findNoShowCandidates(LocalDate onOrBeforeDate);

    /** Réservations d'une société garante, sorties (checked_out) sur la période — utilisé par la facturation société groupée. */
    List<BookingSummary> findByCompanyCheckedOutBetween(Long companyId, LocalDate from, LocalDate to);

    /** Réservations effectivement sorties (statut checked_out) le {@code date} donné — utilisé par l'enquête de satisfaction post-séjour. */
    List<BookingSummary> findCheckedOutOn(LocalDate date);

    /** Chiffre d'affaires groupé par canal/segment (Booking#source) sur les sorties de la période — rapport de production par canal. */
    List<SourceRevenuePoint> revenueBySourceCheckedOutBetween(LocalDate start, LocalDate end);

    /** Comme {@link #revenueBySourceCheckedOutBetween(LocalDate, LocalDate)}, restreint à un établissement. */
    List<SourceRevenuePoint> revenueBySourceCheckedOutBetween(Long propertyId, LocalDate start, LocalDate end);

    /** Réservations individuelles d'un groupe/allotement — la "rooming list" (voir com.pms.hotel.groupbooking). */
    List<BookingSummary> findByGroupId(Long groupId);

    /**
     * Marque une réservation no-show et calcule le frais applicable (même
     * logique que l'annulation — un no-show est traité comme une annulation
     * à délai nul). Idempotent : ne recalcule pas le frais si déjà posé.
     */
    BookingSummary markNoShow(Long bookingId);
}
