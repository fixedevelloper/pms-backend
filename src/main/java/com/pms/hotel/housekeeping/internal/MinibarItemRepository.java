package com.pms.hotel.housekeeping.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinibarItemRepository extends JpaRepository<MinibarItem, Long> {

    List<MinibarItem> findByActiveTrueOrderByName();
}
