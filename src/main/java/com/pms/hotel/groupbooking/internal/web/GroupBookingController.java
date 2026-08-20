package com.pms.hotel.groupbooking.internal.web;

import com.pms.hotel.groupbooking.GroupSummary;
import com.pms.hotel.groupbooking.internal.GroupBookingService;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.CreateAllotmentRequest;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.CreateGroupRequest;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.UpdateAllotmentRequest;
import com.pms.hotel.groupbooking.internal.web.GroupBookingRequests.UpdateGroupRequest;
import com.pms.hotel.property.CurrentProperty;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
class GroupBookingController {

    private final GroupBookingService groupBookingService;
    private final CurrentProperty currentProperty;

    @GetMapping
    public List<GroupSummary> index() {
        return groupBookingService.list(currentProperty.resolve());
    }

    @GetMapping("/{id}")
    public GroupSummary show(@PathVariable Long id) {
        return groupBookingService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupSummary store(@Valid @RequestBody CreateGroupRequest request) {
        return groupBookingService.create(currentProperty.resolve(), request);
    }

    @PutMapping("/{id}")
    public GroupSummary update(@PathVariable Long id, @Valid @RequestBody UpdateGroupRequest request) {
        return groupBookingService.update(id, request);
    }

    @PostMapping("/{id}/allotments")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupSummary addAllotment(@PathVariable Long id, @Valid @RequestBody CreateAllotmentRequest request) {
        return groupBookingService.addAllotment(id, request);
    }

    @PutMapping("/{id}/allotments/{allotmentId}")
    public GroupSummary updateAllotment(
            @PathVariable Long id, @PathVariable Long allotmentId, @Valid @RequestBody UpdateAllotmentRequest request) {
        return groupBookingService.updateAllotment(id, allotmentId, request);
    }

    @DeleteMapping("/{id}/allotments/{allotmentId}")
    public GroupSummary removeAllotment(@PathVariable Long id, @PathVariable Long allotmentId) {
        groupBookingService.removeAllotment(id, allotmentId);
        return groupBookingService.getById(id);
    }
}
