package com.pms.hotel.settings.internal;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, Long> {

    Optional<Setting> findByKey(String key);
}
