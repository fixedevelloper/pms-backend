package com.pms.hotel.channelmanager.internal;

import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.ExternalBookingUpsert;
import com.pms.hotel.channelmanager.internal.web.ChannelWebhookPayload;
import com.pms.hotel.channelmanager.internal.web.ChannelWebhookPayload.Data;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.guest.GuestUpsertRequest;
import com.pms.hotel.room.RoomApi;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChannelManagerService {

    private final GuestApi guestApi;
    private final RoomApi roomApi;
    private final BookingApi bookingApi;

    public void handle(ChannelWebhookPayload payload) {
        if ("cancel".equals(payload.action())) {
            bookingApi.cancelByExternalReference(payload.data().id());
            return;
        }

        Data data = payload.data();

        GuestSummary guest = guestApi.findOrCreateByEmail(new GuestUpsertRequest(
                data.customer().firstName(), data.customer().lastName(),
                data.customer().email(), data.customer().phone(), null));

        Long roomId = roomApi.findByExternalChannelRoomId(data.channelRoomId())
                .map(com.pms.hotel.room.RoomDetails::id)
                .orElse(null);

        bookingApi.upsertFromExternalChannel(new ExternalBookingUpsert(
                data.id(),
                guest.id(),
                data.arrivalDate().atStartOfDay().toInstant(ZoneOffset.UTC),
                data.departureDate().atStartOfDay().toInstant(ZoneOffset.UTC),
                data.otaName(),
                data.amount(),
                data.taxAmount(),
                roomId,
                data.amountPerNight()));
    }
}
