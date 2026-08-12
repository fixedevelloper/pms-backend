package com.pms.hotel.room.internal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    boolean existsByName(String name);
}
