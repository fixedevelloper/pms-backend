package com.pms.hotel.groupbooking;

import java.time.LocalDate;
import java.util.List;

public record GroupSummary(
        Long id,
        Long propertyId,
        String name,
        Long companyId,
        String contactName,
        String contactEmail,
        String contactPhone,
        LocalDate checkIn,
        LocalDate checkOut,
        String status,
        String notes,
        List<GroupRoomAllotmentView> allotments) {
}
