package com.pms.hotel.housekeeping.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LostFoundItemRepository extends JpaRepository<LostFoundItem, Long> {

    List<LostFoundItem> findByStatusOrderByCreatedAtDesc(String status);

    List<LostFoundItem> findAllByOrderByCreatedAtDesc();
}
