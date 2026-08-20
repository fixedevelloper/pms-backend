package com.pms.hotel.room.internal;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    boolean existsByName(String name);

    List<RoomType> findByPropertyId(Long propertyId, Sort sort);
}
