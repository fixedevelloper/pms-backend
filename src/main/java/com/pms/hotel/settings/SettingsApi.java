package com.pms.hotel.settings;

import java.util.Optional;

/**
 * Public entry point into the settings module for the rest of the
 * application. Other modules must depend only on this interface, never on
 * {@code com.pms.hotel.settings.internal} types.
 */
public interface SettingsApi {

    /** True only if the setting exists and its value is exactly "1" (matches settingService.ts's boolean-as-string convention). */
    boolean isEnabled(String key);

    /** Raw string value, empty if the key was never configured (settings have no seeded defaults). */
    Optional<String> get(String key);
}
