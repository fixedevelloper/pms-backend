package com.pms.hotel.groupbooking;

public record GroupRoomAllotmentView(Long id, Long roomTypeId, Long ratePlanId, int allottedRooms, String notes) {
}
