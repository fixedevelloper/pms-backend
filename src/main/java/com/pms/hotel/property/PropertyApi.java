package com.pms.hotel.property;

import java.util.List;

/**
 * Public entry point into the property module for the rest of the
 * application. Other modules must depend only on this interface (and the
 * records it exposes), never on {@code com.pms.hotel.property.internal} types.
 */
public interface PropertyApi {

    PropertySummary getById(Long propertyId);

    List<PropertySummary> findAllActive();

    List<Long> findAllActivePropertyIds();

    /** Établissements explicitement accordés à cet utilisateur (voir UserPropertyAccess) — ignore les accès implicites (rôle large, amorçage mono-établissement, voir CurrentProperty). */
    List<Long> findGrantedPropertyIds(Long userId);
}
