package com.pms.hotel.housekeeping.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinibarConsumptionRepository extends JpaRepository<MinibarConsumption, Long> {

    List<MinibarConsumption> findByRoomIdOrderByCreatedAtDesc(Long roomId);
}
