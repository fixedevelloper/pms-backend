package com.pms.hotel.maintenance.internal.web;

import com.pms.hotel.maintenance.MaintenanceTicketView;
import com.pms.hotel.maintenance.internal.MaintenanceService;
import com.pms.hotel.maintenance.internal.web.MaintenanceRequests.CreateTicketRequest;
import com.pms.hotel.maintenance.internal.web.MaintenanceRequests.UpdateTicketRequest;
import com.pms.hotel.property.CurrentProperty;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/maintenance-tickets")
@RequiredArgsConstructor
class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final RoomApi roomApi;
    private final CurrentUser currentUser;
    private final CurrentProperty currentProperty;

    @GetMapping
    public List<MaintenanceTicketView> index(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long roomId) {
        return maintenanceService.list(roomApi.findRoomIdsByProperty(currentProperty.resolve()), status, roomId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('manage housekeeping') or hasAuthority('manage rooms')")
    public MaintenanceTicketView store(@Valid @RequestBody CreateTicketRequest request) {
        return maintenanceService.create(
                roomApi.findRoomIdsByProperty(currentProperty.resolve()),
                request.roomId(), request.title(), request.description(), request.priority(), currentUser.userId());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('manage housekeeping') or hasAuthority('manage rooms')")
    public MaintenanceTicketView update(@PathVariable Long id, @Valid @RequestBody UpdateTicketRequest request) {
        return maintenanceService.update(
                id, request.title(), request.description(), request.priority(),
                request.status(), request.assignedTo(), request.cost());
    }
}
