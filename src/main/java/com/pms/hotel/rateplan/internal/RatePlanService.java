package com.pms.hotel.rateplan.internal;

import com.pms.hotel.rateplan.RatePlanApi;
import com.pms.hotel.rateplan.RatePlanPriceChangedEvent;
import com.pms.hotel.rateplan.RatePlanSummary;
import com.pms.hotel.rateplan.internal.web.RatePlanRequests.CreateRatePlanRequest;
import com.pms.hotel.rateplan.internal.web.RatePlanRequests.UpdateRatePlanRequest;
import com.pms.hotel.shared.exception.ResourceNotFoundException;
import com.pms.hotel.shared.security.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RatePlanService implements RatePlanApi {

    private final RatePlanRepository ratePlanRepository;
    private final ApplicationEventPublisher events;
    private final CurrentUser currentUser;

    @Override
    @Transactional(readOnly = true)
    public RatePlanSummary getById(Long ratePlanId) {
        return findEntity(ratePlanId).toSummary();
    }

    /** {@code propertyRoomTypeIds} : types de chambre de l'établissement courant (voir RoomApi#findRoomTypeIdsByProperty) — {@code roomTypeId}, s'il est fourni, doit en faire partie (vérifié par l'appelant). */
    @Transactional(readOnly = true)
    public List<RatePlanSummary> list(List<Long> propertyRoomTypeIds, Long roomTypeId) {
        List<RatePlan> plans = roomTypeId != null
                ? ratePlanRepository.findByRoomTypeIdOrderByName(roomTypeId)
                : ratePlanRepository.findByRoomTypeIdInOrderByRoomTypeIdAscNameAsc(propertyRoomTypeIds);
        return plans.stream().map(RatePlan::toSummary).toList();
    }

    /** Tarifs actifs d'un type de chambre donné, sans filtrage par propriété — réservé au Booking Engine public (RatePlanApi), qui résout déjà son propertyId autrement (voir sa Javadoc). */
    @Override
    @Transactional(readOnly = true)
    public List<RatePlanSummary> listActive(Long roomTypeId) {
        return ratePlanRepository.findByRoomTypeIdOrderByName(roomTypeId).stream()
                .map(RatePlan::toSummary)
                .filter(RatePlanSummary::active)
                .toList();
    }

    public RatePlanSummary create(CreateRatePlanRequest request) {
        RatePlan ratePlan = new RatePlan();
        ratePlan.setRoomTypeId(request.roomTypeId());
        ratePlan.setName(request.name());
        ratePlan.setDescription(request.description());
        ratePlan.setPricePerNight(request.pricePerNight());
        ratePlan.setBreakfastIncluded(Boolean.TRUE.equals(request.breakfastIncluded()));
        ratePlan.setCancellationPolicy(request.cancellationPolicy());
        ratePlan.setFreeCancellationDays(request.freeCancellationDays());
        ratePlan.setCancellationFeePercent(request.cancellationFeePercent());
        ratePlan.setActive(true);
        RatePlanSummary summary = ratePlanRepository.save(ratePlan).toSummary();
        events.publishEvent(new RatePlanPriceChangedEvent(summary.id(), summary.roomTypeId(), summary.pricePerNight(), currentUser.userId()));
        return summary;
    }

    public RatePlanSummary update(Long id, UpdateRatePlanRequest request) {
        RatePlan ratePlan = findEntity(id);
        boolean priceChanged = request.pricePerNight() != null && request.pricePerNight().compareTo(ratePlan.getPricePerNight()) != 0;

        if (request.name() != null) ratePlan.setName(request.name());
        if (request.description() != null) ratePlan.setDescription(request.description());
        if (request.pricePerNight() != null) ratePlan.setPricePerNight(request.pricePerNight());
        if (request.breakfastIncluded() != null) ratePlan.setBreakfastIncluded(request.breakfastIncluded());
        if (request.cancellationPolicy() != null) ratePlan.setCancellationPolicy(request.cancellationPolicy());
        if (request.freeCancellationDays() != null) ratePlan.setFreeCancellationDays(request.freeCancellationDays());
        if (request.cancellationFeePercent() != null) ratePlan.setCancellationFeePercent(request.cancellationFeePercent());
        if (request.active() != null) ratePlan.setActive(request.active());

        RatePlanSummary summary = ratePlanRepository.save(ratePlan).toSummary();
        if (priceChanged) {
            events.publishEvent(new RatePlanPriceChangedEvent(summary.id(), summary.roomTypeId(), summary.pricePerNight(), currentUser.userId()));
        }
        return summary;
    }

    private RatePlan findEntity(Long id) {
        return ratePlanRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tarif", id));
    }
}
