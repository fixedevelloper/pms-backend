package com.pms.hotel.channelmanager.internal;

import com.pms.hotel.booking.BookingApi;
import com.pms.hotel.booking.ExternalBookingUpsert;
import com.pms.hotel.channelmanager.ChannelRoomMappingView;
import com.pms.hotel.channelmanager.ChannelSummary;
import com.pms.hotel.channelmanager.internal.web.ChannelWebhookPayload;
import com.pms.hotel.channelmanager.internal.web.ChannelWebhookPayload.Data;
import com.pms.hotel.guest.GuestApi;
import com.pms.hotel.guest.GuestSummary;
import com.pms.hotel.guest.GuestUpsertRequest;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import java.time.ZoneOffset;
import java.util.List;
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
    private final ChannelRepository channelRepository;
    private final ChannelRoomMappingRepository mappingRepository;

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

    @Transactional(readOnly = true)
    public List<ChannelSummary> listChannels() {
        return channelRepository.findAll().stream().map(Channel::toSummary).toList();
    }

    public ChannelSummary createChannel(String name, String webhookUrl, String webhookSecret) {
        Channel channel = new Channel();
        channel.setName(name);
        channel.setWebhookUrl(webhookUrl);
        channel.setWebhookSecret(webhookSecret);
        channel.setActive(true);
        return channelRepository.save(channel).toSummary();
    }

    public ChannelSummary updateChannel(Long id, String name, String webhookUrl, String webhookSecret, Boolean active) {
        Channel channel = findChannel(id);
        if (name != null) channel.setName(name);
        if (webhookUrl != null) channel.setWebhookUrl(webhookUrl);
        if (webhookSecret != null) channel.setWebhookSecret(webhookSecret);
        if (active != null) channel.setActive(active);
        return channelRepository.save(channel).toSummary();
    }

    public void deleteChannel(Long id) {
        channelRepository.delete(findChannel(id));
    }

    /** {@code propertyRoomIds} : chambres de l'établissement courant (voir RoomApi#findRoomIdsByProperty) — un canal n'a pas de propertyId direct (voir Channel), donc le cloisonnement passe ici par la chambre mappée. */
    @Transactional(readOnly = true)
    public List<ChannelRoomMappingView> listMappings(List<Long> propertyRoomIds, Long channelId) {
        return mappingRepository.findByChannelId(channelId).stream()
                .filter(m -> propertyRoomIds.contains(m.getRoomId()))
                .map(m -> m.toView(roomApi.getById(m.getRoomId()).roomNumber()))
                .toList();
    }

    public ChannelRoomMappingView createMapping(List<Long> propertyRoomIds, Long channelId, Long roomId, String externalRoomId) {
        if (!propertyRoomIds.contains(roomId)) {
            throw new BusinessRuleException("Cette chambre n'appartient pas à l'établissement courant.");
        }
        if (mappingRepository.existsByChannelIdAndRoomId(channelId, roomId)) {
            throw new BusinessRuleException("Cette chambre est déjà mappée sur ce canal.");
        }
        Channel channel = findChannel(channelId);
        String roomNumber = roomApi.getById(roomId).roomNumber(); // 404 si la chambre n'existe pas
        ChannelRoomMapping mapping = new ChannelRoomMapping();
        mapping.setChannel(channel);
        mapping.setRoomId(roomId);
        mapping.setExternalRoomId(externalRoomId);
        return mappingRepository.save(mapping).toView(roomNumber);
    }

    public void deleteMapping(List<Long> propertyRoomIds, Long channelId, Long mappingId) {
        ChannelRoomMapping mapping = mappingRepository.findById(mappingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Mapping", mappingId));
        if (!mapping.getChannel().getId().equals(channelId)) {
            throw new ResourceNotFoundException("Ce mapping n'appartient pas au canal indiqué.");
        }
        if (!propertyRoomIds.contains(mapping.getRoomId())) {
            throw new BusinessRuleException("Cette chambre n'appartient pas à l'établissement courant.");
        }
        mappingRepository.delete(mapping);
    }

    private Channel findChannel(Long id) {
        return channelRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Canal", id));
    }
}
