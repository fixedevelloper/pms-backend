package com.pms.hotel.notification.application;

import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Déclenche les deux relances automatisées qui n'ont pas d'événement métier
 * naturel auquel s'accrocher (contrairement à la confirmation, envoyée à la
 * création — voir BookingEventListener) : la veille-de-veille de l'arrivée,
 * et le lendemain du départ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private static final List<String> UPCOMING_STATUSES = List.of("pending", "confirmed");

    private final BookingApi bookingApi;
    private final GuestApi guestApi;
    private final NotificationService notificationService;

    /** Tous les jours à 8h — relance pré-arrivée pour les arrivées prévues dans 2 jours. */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendPreArrivalReminders() {
        LocalDate target = LocalDate.now().plusDays(2);
        List<BookingSummary> arrivals = bookingApi.findArrivalsOn(target);
        int sent = 0;
        for (BookingSummary booking : arrivals) {
            if (!UPCOMING_STATUSES.contains(booking.status())) continue;
            GuestSummary guest = guestApi.getById(booking.guestId());
            notificationService.sendPreArrivalReminder(guest, booking);
            sent++;
        }
        log.info("Relances pré-arrivée : {} envoyée(s) pour le {}", sent, target);
    }

    /** Tous les jours à 10h — enquête de satisfaction pour les départs de la veille. */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendPostStaySurveys() {
        LocalDate target = LocalDate.now().minusDays(1);
        List<BookingSummary> departures = bookingApi.findCheckedOutOn(target);
        for (BookingSummary booking : departures) {
            GuestSummary guest = guestApi.getById(booking.guestId());
            notificationService.sendPostStaySurvey(guest, booking);
        }
        log.info("Enquêtes post-séjour : {} envoyée(s) pour le départ du {}", departures.size(), target);
    }
}
