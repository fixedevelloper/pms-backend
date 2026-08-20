package com.pms.hotel.groupbooking;

/**
 * Public entry point into the groupbooking module for the rest of the
 * application. Other modules must depend only on this interface (and the
 * records it exposes), never on {@code com.pms.hotel.groupbooking.internal} types.
 */
public interface GroupBookingApi {

    GroupSummary getById(Long groupId);
}
