package com.pms.hotel.rateplan.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RatePlanRepository extends JpaRepository<RatePlan, Long> {

    List<RatePlan> findByRoomTypeIdOrderByName(Long roomTypeId);

    List<RatePlan> findAllByOrderByRoomTypeIdAscNameAsc();

    /** Comme {@link #findAllByOrderByRoomTypeIdAscNameAsc()}, restreint aux types de chambre d'un établissement (RatePlan n'a pas de propertyId direct). */
    List<RatePlan> findByRoomTypeIdInOrderByRoomTypeIdAscNameAsc(List<Long> roomTypeIds);
}
