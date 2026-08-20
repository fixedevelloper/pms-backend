package com.pms.hotel.groupbooking.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingGroupRepository extends JpaRepository<BookingGroup, Long> {

    List<BookingGroup> findByPropertyIdOrderByCheckInDesc(Long propertyId);
}
