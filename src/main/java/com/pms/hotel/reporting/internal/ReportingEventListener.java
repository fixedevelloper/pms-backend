package com.pms.hotel.reporting.internal;

import com.pms.hotel.rateplan.RatePlanPriceChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ReportingEventListener {

    private final RateChangeAuditLogRepository rateChangeAuditLogRepository;

    @ApplicationModuleListener
    void onRatePlanPriceChanged(RatePlanPriceChangedEvent event) {
        RateChangeAuditLog log = new RateChangeAuditLog();
        log.setRatePlanId(event.ratePlanId());
        log.setRoomTypeId(event.roomTypeId());
        log.setNewPrice(event.pricePerNight());
        log.setChangedByUserId(event.changedByUserId());
        rateChangeAuditLogRepository.save(log);
    }
}
