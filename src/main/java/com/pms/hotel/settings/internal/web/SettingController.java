package com.pms.hotel.settings.internal.web;

import com.pms.hotel.settings.internal.Setting;
import com.pms.hotel.settings.internal.SettingRepository;
import jakarta.validation.constraints.NotNull;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
class SettingController {

    private final SettingRepository settingRepository;

    @GetMapping
    public Map<String, String> index() {
        Map<String, String> result = new LinkedHashMap<>();
        settingRepository.findAll().forEach(setting -> result.put(setting.getKey(), setting.getValue()));
        return result;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('settings.update')")
    public Map<String, String> store(@RequestBody UpdateSettingsRequest request) {
        request.settings().forEach((key, value) -> {
            Setting setting = settingRepository.findByKey(key).orElseGet(() -> {
                Setting created = new Setting();
                created.setKey(key);
                return created;
            });
            setting.setValue(value);
            settingRepository.save(setting);
        });
        return index();
    }

    record UpdateSettingsRequest(@NotNull Map<String, String> settings) {
    }
}
