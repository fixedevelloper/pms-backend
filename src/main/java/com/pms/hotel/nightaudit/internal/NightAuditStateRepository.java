package com.pms.hotel.nightaudit.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NightAuditStateRepository extends JpaRepository<NightAuditState, Long> {

    Optional<NightAuditState> findFirstByOrderByIdAsc();
}
