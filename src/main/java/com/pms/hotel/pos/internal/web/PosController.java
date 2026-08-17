package com.pms.hotel.pos.internal.web;

import com.pms.hotel.pos.ExtraChargeLine;
import com.pms.hotel.pos.internal.PosService;
import com.pms.hotel.pos.internal.PosService.RoomCheckResult;
import com.pms.hotel.pos.internal.web.PosRequests.ChargeToRoomRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by monokek-spring's {@code ordering.pms.PmsClient}, forwarding the requesting till
 * user's own bearer token (see that module's package-info) — so this is reachable by any staff
 * account with a POS-facing role. Restricted to the roles that actually operate a till; a kitchen
 * account has no legitimate reason to look up or bill a guest's room folio.
 */
@RestController
@RequestMapping("/api/v1/restaurant")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'CASHIER', 'WAITER')")
class PosController {

    private final PosService posService;

    @GetMapping("/check-room/{roomNumber}")
    public RoomCheckResult checkRoomStatus(@PathVariable String roomNumber) {
        return posService.checkRoomStatus(roomNumber);
    }

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.CREATED)
    public ExtraChargeLine chargeToRoom(@Valid @RequestBody ChargeToRoomRequest request) {
        return posService.chargeToRoom(
                request.bookingId(), request.department(), request.itemName(),
                request.quantity(), request.unitPrice(), request.externalOrderId());
    }
}
