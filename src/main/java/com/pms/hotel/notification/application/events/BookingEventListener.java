package com.pms.hotel.notification.application.events;

import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.BookingCreatedEvent;
import com.pms.hotel.booking.BookingSummary;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final BookingApi bookingApi;
    private final NotificationService notificationService;
    private final GuestApi guestApi;

    @EventListener
    public void handleBookingCreated(BookingCreatedEvent event) {
        BookingSummary booking = bookingApi.getById(event.bookingId());
        GuestSummary guest = guestApi.getById(booking.guestId());
        notificationService.sendBookingConfirmation(guest, booking);
    }
}
