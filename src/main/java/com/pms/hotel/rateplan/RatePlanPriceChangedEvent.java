package com.pms.hotel.rateplan;

import java.math.BigDecimal;

/** Publié par RatePlanService quand pricePerNight change — s'applique à toutes les chambres du type concerné. */
public record RatePlanPriceChangedEvent(Long ratePlanId, Long roomTypeId, BigDecimal pricePerNight) {
}
