package com.pms.hotel.channelmanager.internal;

import com.pms.hotel.booking.BookingAvailabilityChangedEvent;
import com.pms.hotel.rateplan.RatePlanPriceChangedEvent;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.room.RoomAvailabilityChangedEvent;
import com.pms.hotel.room.RoomDetails;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Écoute les événements d'autres modules qui affectent la disponibilité/les
 * tarifs vendables, et pousse un événement ARI vers chaque canal actif mappé
 * sur la ou les chambres concernées. N'implémente aucun protocole OTA
 * propriétaire (Booking.com/Expedia) — c'est un point de branchement
 * générique consommable par un agrégateur (SiteMinder, RateGain...) ou un
 * webhook de test.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelPushService {

    private final ChannelRoomMappingRepository mappingRepository;
    private final RoomApi roomApi;
    private final ChannelPushClient pushClient;

    @ApplicationModuleListener
    void onBookingAvailabilityChanged(BookingAvailabilityChangedEvent event) {
        pushToMappedChannels(event.roomId(), event.action(), event.checkIn(), event.checkOut(), null);
    }

    @ApplicationModuleListener
    void onRoomAvailabilityChanged(RoomAvailabilityChangedEvent event) {
        pushToMappedChannels(event.roomId(), event.action(), event.startDate(), event.endDate(), null);
    }

    @ApplicationModuleListener
    void onRatePlanPriceChanged(RatePlanPriceChangedEvent event) {
        List<RoomDetails> roomsOfType = roomApi.findAll().stream()
                .filter(r -> r.roomTypeId().equals(event.roomTypeId()))
                .toList();
        for (RoomDetails room : roomsOfType) {
            pushToMappedChannels(room.id(), "rate_changed", null, null, event.pricePerNight());
        }
    }

    private void pushToMappedChannels(Long roomId, String action, java.time.LocalDate startDate, java.time.LocalDate endDate, java.math.BigDecimal pricePerNight) {
        List<ChannelRoomMapping> mappings = mappingRepository.findByRoomId(roomId);
        if (mappings.isEmpty()) {
            return;
        }
        String roomNumber = roomApi.getById(roomId).roomNumber();
        for (ChannelRoomMapping mapping : mappings) {
            if (!mapping.getChannel().isActive()) {
                continue;
            }
            AriPushPayload payload = new AriPushPayload(
                    action, mapping.getExternalRoomId(), roomNumber, startDate, endDate, pricePerNight, Instant.now());
            pushClient.push(mapping.getChannel(), payload);
        }
    }
}
