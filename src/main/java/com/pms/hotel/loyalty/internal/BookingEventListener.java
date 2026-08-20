package com.pms.hotel.loyalty.internal;

import com.pms.hotel.booking.BookingCheckedOutEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BookingEventListener {

    private final LoyaltyService loyaltyService;

    @ApplicationModuleListener
    void onBookingCheckedOut(BookingCheckedOutEvent event) {
        loyaltyService.earnPoints(event.guestId(), event.bookingId(), event.totalAmount());
    }
}
