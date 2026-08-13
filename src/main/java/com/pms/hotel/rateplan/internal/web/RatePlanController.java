package com.pms.hotel.rateplan.internal.web;

import com.pms.hotel.rateplan.RatePlanSummary;
import com.pms.hotel.rateplan.internal.RatePlanService;
import com.pms.hotel.rateplan.internal.web.RatePlanRequests.CreateRatePlanRequest;
import com.pms.hotel.rateplan.internal.web.RatePlanRequests.UpdateRatePlanRequest;
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

    @GetMapping
    public List<RatePlanSummary> index(@RequestParam(required = false) Long roomTypeId) {
        return ratePlanService.list(roomTypeId);
    }

    @GetMapping("/{id}")
    public RatePlanSummary show(@PathVariable Long id) {
        return ratePlanService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RatePlanSummary store(@Valid @RequestBody CreateRatePlanRequest request) {
        return ratePlanService.create(request);
    }

    @PutMapping("/{id}")
    public RatePlanSummary update(@PathVariable Long id, @Valid @RequestBody UpdateRatePlanRequest request) {
        return ratePlanService.update(id, request);
    }
}
