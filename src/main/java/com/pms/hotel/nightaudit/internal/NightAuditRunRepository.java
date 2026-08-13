package com.pms.hotel.nightaudit.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NightAuditRunRepository extends JpaRepository<NightAuditRun, Long> {

    boolean existsByBusinessDate(LocalDate businessDate);

    Optional<NightAuditRun> findFirstByOrderByBusinessDateDesc();

    List<NightAuditRun> findAllByOrderByBusinessDateDesc();
}
