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

    BigDecimal sumTotalAmountByStatus(String status);

    BigDecimal sumRevenueForStatusCreatedBetween(String status, Instant from, Instant to);

    List<DailyRevenuePoint> revenueByCheckoutDateBetween(LocalDate start, LocalDate end);
}
