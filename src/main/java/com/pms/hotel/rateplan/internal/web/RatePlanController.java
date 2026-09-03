package com.pms.hotel.rateplan.internal.web;

import com.pms.hotel.property.CurrentProperty;
import com.pms.hotel.rateplan.RatePlanSummary;
import com.pms.hotel.rateplan.internal.RatePlanService;
import com.pms.hotel.rateplan.internal.web.RatePlanRequests.CreateRatePlanRequest;
import com.pms.hotel.rateplan.internal.web.RatePlanRequests.UpdateRatePlanRequest;
import com.pms.hotel.room.RoomApi;
import com.pms.hotel.shared.exception.BusinessRuleException;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rate-plans")
@RequiredArgsConstructor
class RatePlanController {

    private final RatePlanService ratePlanService;
    private final RoomApi roomApi;
    private final CurrentProperty currentProperty;

    @GetMapping
    public List<RatePlanSummary> index(@RequestParam(required = false) Long roomTypeId) {
        List<Long> propertyRoomTypeIds = roomApi.findRoomTypeIdsByProperty(currentProperty.resolve());
        if (roomTypeId != null && !propertyRoomTypeIds.contains(roomTypeId)) {
            throw new BusinessRuleException("Ce type de chambre n'appartient pas à l'établissement courant.");
        }
        return ratePlanService.list(propertyRoomTypeIds, roomTypeId);
    }

    @GetMapping("/{id}")
    public RatePlanSummary show(@PathVariable Long id) {
        RatePlanSummary summary = ratePlanService.getById(id);
        requireRoomTypeInCurrentProperty(summary.roomTypeId());
        return summary;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatePlanSummary store(@Valid @RequestBody CreateRatePlanRequest request) {
        requireRoomTypeInCurrentProperty(request.roomTypeId());
        return ratePlanService.create(request);
    }

    @PutMapping("/{id}")
    public RatePlanSummary update(@PathVariable Long id, @Valid @RequestBody UpdateRatePlanRequest request) {
        requireRoomTypeInCurrentProperty(ratePlanService.getById(id).roomTypeId());
        return ratePlanService.update(id, request);
    }

    private void requireRoomTypeInCurrentProperty(Long roomTypeId) {
        if (!roomApi.findRoomTypeIdsByProperty(currentProperty.resolve()).contains(roomTypeId)) {
            throw new BusinessRuleException("Ce type de chambre n'appartient pas à l'établissement courant.");
        }
    }
}
