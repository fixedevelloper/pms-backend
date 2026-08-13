package com.pms.hotel.settings.internal;

import com.pms.hotel.settings.SettingsApi;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService implements SettingsApi {

    private final SettingRepository settingRepository;

    @Override
    public boolean isEnabled(String key) {
        return settingRepository.findByKey(key).map(s -> "1".equals(s.getValue())).orElse(false);
    }

    @Override
    public Optional<String> get(String key) {
        return settingRepository.findByKey(key).map(Setting::getValue);
    }
}
