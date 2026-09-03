package com.pms.hotel.channelmanager.internal.web;

import com.pms.hotel.channelmanager.ChannelRoomMappingView;
import com.pms.hotel.channelmanager.ChannelSummary;
import com.pms.hotel.channelmanager.internal.ChannelManagerService;
import com.pms.hotel.channelmanager.internal.web.ChannelRequests.CreateChannelRequest;
import com.pms.hotel.channelmanager.internal.web.ChannelRequests.CreateMappingRequest;
import com.pms.hotel.channelmanager.internal.web.ChannelRequests.UpdateChannelRequest;
import com.pms.hotel.property.CurrentProperty;
import com.pms.hotel.room.RoomApi;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Administration des canaux de distribution (OTA) et de leur mapping vers les
 * chambres — DISTINCT de {@link ChannelManagerController} (webhook entrant,
 * {@code /api/v1/channel-manager/**}, en accès libre). Ces routes-ci sont
 * authentifiées comme n'importe quelle route staff (voir SecurityConfig :
 * seul {@code /api/v1/channel-manager/**} est en permitAll).
 */
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
class ChannelController {

    private final ChannelManagerService channelManagerService;
    private final RoomApi roomApi;
    private final CurrentProperty currentProperty;

    @GetMapping
    public List<ChannelSummary> index() {
        return channelManagerService.listChannels();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('manage rooms')")
    public ChannelSummary store(@Valid @RequestBody CreateChannelRequest request) {
        return channelManagerService.createChannel(request.name(), request.webhookUrl(), request.webhookSecret());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('manage rooms')")
    public ChannelSummary update(@PathVariable Long id, @Valid @RequestBody UpdateChannelRequest request) {
        return channelManagerService.updateChannel(id, request.name(), request.webhookUrl(), request.webhookSecret(), request.active());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('manage rooms')")
    public void delete(@PathVariable Long id) {
        channelManagerService.deleteChannel(id);
    }

    @GetMapping("/{id}/mappings")
    public List<ChannelRoomMappingView> indexMappings(@PathVariable Long id) {
        return channelManagerService.listMappings(roomApi.findRoomIdsByProperty(currentProperty.resolve()), id);
    }

    @PostMapping("/{id}/mappings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('manage rooms')")
    public ChannelRoomMappingView storeMapping(@PathVariable Long id, @Valid @RequestBody CreateMappingRequest request) {
        return channelManagerService.createMapping(
                roomApi.findRoomIdsByProperty(currentProperty.resolve()), id, request.roomId(), request.externalRoomId());
    }

    @DeleteMapping("/{id}/mappings/{mappingId}")
    @PreAuthorize("hasAuthority('manage rooms')")
    public void deleteMapping(@PathVariable Long id, @PathVariable Long mappingId) {
        channelManagerService.deleteMapping(roomApi.findRoomIdsByProperty(currentProperty.resolve()), id, mappingId);
    }
}
