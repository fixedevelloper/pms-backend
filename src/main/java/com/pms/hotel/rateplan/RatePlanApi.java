package com.pms.hotel.rateplan;

/**
 * Public entry point into the rate plan module for the rest of the
 * application. Other modules must depend only on this interface (and the
 * records it exposes), never on {@code com.pms.hotel.rateplan.internal} types.
 */
public interface RatePlanApi {

    RatePlanSummary getById(Long ratePlanId);
}
