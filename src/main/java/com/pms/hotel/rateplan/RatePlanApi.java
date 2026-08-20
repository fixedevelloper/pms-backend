package com.pms.hotel.rateplan;

import java.util.List;

/**
 * Public entry point into the rate plan module for the rest of the
 * application. Other modules must depend only on this interface (and the
 * records it exposes), never on {@code com.pms.hotel.rateplan.internal} types.
 */
public interface RatePlanApi {

    RatePlanSummary getById(Long ratePlanId);

    /** Tarifs actifs d'un type de chambre — utilisé par le Booking Engine public (jamais de tarif désactivé proposé au client). */
    List<RatePlanSummary> listActive(Long roomTypeId);
}
