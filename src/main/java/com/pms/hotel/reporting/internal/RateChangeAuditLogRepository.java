package com.pms.hotel.reporting.internal;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateChangeAuditLogRepository extends JpaRepository<RateChangeAuditLog, Long> {

    List<RateChangeAuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
