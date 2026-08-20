package com.pms.hotel.company;

import java.util.List;

/**
 * Public entry point into the company module for the rest of the
 * application. Other modules must depend only on this interface (and the
 * records it exposes), never on {@code com.pms.hotel.company.internal} types.
 */
public interface CompanyApi {

    CompanySummary getById(Long id);

    List<CompanySummary> findAllActive();
}
