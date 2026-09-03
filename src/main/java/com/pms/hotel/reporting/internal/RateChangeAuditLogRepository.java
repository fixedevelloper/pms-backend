package com.pms.hotel.reporting.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateChangeAuditLogRepository extends JpaRepository<RateChangeAuditLog, Long> {

    List<RateChangeAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    /** Comme {@link #findByCreatedAtBetweenOrderByCreatedAtDesc}, restreint aux types de chambre d'un établissement (RateChangeAuditLog n'a pas de propertyId direct). */
    List<RateChangeAuditLog> findByRoomTypeIdInAndCreatedAtBetweenOrderByCreatedAtDesc(List<Long> roomTypeIds, Instant from, Instant to);
}
