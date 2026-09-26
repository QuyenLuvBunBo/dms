package vn.edu.hust.dms.common.settings;

import java.util.Arrays;
import java.util.Optional;

/**
 * Every setting the application reads. The rows themselves are seeded by Flyway
 * (V2__seed_settings.sql); later phases add keys here and in a new migration.
 */
public enum SettingKey {
    /** BR-05: hours a bed offer stays open. */
    OFFER_HOURS("offer_hours", SettingType.HOURS),
    /** BR-11: violations in one term that create an eviction proposal. */
    WARNING_THRESHOLD("warning_threshold", SettingType.INTEGER),
    /** BR-10: repair deadline by priority. */
    REPAIR_DUE_HOURS_URGENT("repair_due_hours_urgent", SettingType.HOURS),
    REPAIR_DUE_HOURS_NORMAL("repair_due_hours_normal", SettingType.HOURS),
    REPAIR_DUE_HOURS_LOW("repair_due_hours_low", SettingType.HOURS);

    private final String key;
    private final SettingType type;

    SettingKey(String key, SettingType type) {
        this.key = key;
        this.type = type;
    }

    public String key() {
        return key;
    }

    public SettingType type() {
        return type;
    }

    public static Optional<SettingKey> fromKey(String key) {
        return Arrays.stream(values()).filter(candidate -> candidate.key.equals(key)).findFirst();
    }
}
