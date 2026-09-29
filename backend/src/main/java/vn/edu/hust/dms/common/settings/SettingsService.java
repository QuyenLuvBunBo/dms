package vn.edu.hust.dms.common.settings;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.error.NotFoundException;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Typed access to the settings table. Values are read on every call (they are tiny and may be
 * changed by an admin at any time), so business rules always see the current configuration.
 * Reads carry no role check because scheduled jobs call them without a user; the admin API does.
 */
@Service
public class SettingsService {

    private final SettingRepository settings;
    private final Clock clock;

    public SettingsService(SettingRepository settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public int getInt(SettingKey key) {
        return Math.toIntExact(getLong(key));
    }

    @Transactional(readOnly = true)
    public long getLong(SettingKey key) {
        String raw = raw(key);
        try {
            return key.type().parse(raw);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Setting " + key.key() + " holds '" + raw + "', which is not "
                    + key.type().description(), e);
        }
    }

    @Transactional(readOnly = true)
    public Duration getDuration(SettingKey key) {
        ChronoUnit unit = key.type().durationUnit()
                .orElseThrow(() -> new IllegalArgumentException("Setting " + key.key() + " is not a duration"));
        return Duration.of(getLong(key), unit);
    }

    @Transactional(readOnly = true)
    public List<Setting> all() {
        return settings.findAll(Sort.by("key"));
    }

    @Transactional
    public Setting update(String key, String value) {
        SettingKey known = SettingKey.fromKey(key)
                .orElseThrow(() -> new NotFoundException("Unknown setting: " + key));
        String trimmed = value == null ? "" : value.trim();
        try {
            known.type().parse(trimmed);
        } catch (NumberFormatException e) {
            throw new InvalidSettingValueException(known, trimmed);
        }
        Setting setting = settings.findById(known.key()).orElseThrow(() -> notConfigured(known));
        setting.update(trimmed, clock.instant());
        return setting;
    }

    private String raw(SettingKey key) {
        return settings.findById(key.key())
                .map(Setting::getValue)
                .orElseThrow(() -> notConfigured(key));
    }

    private static IllegalStateException notConfigured(SettingKey key) {
        return new IllegalStateException("Setting " + key.key() + " is not configured; check the Flyway seed");
    }
}
