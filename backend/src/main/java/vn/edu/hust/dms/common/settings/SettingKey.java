package vn.edu.hust.dms.common.settings;

import java.util.Arrays;
import java.util.Optional;

/**
 * Every setting the application reads, with the defaults of CLAUDE.md. The rows themselves are
 * seeded by Flyway (V3__spec_v2_roles_and_settings.sql); later keys go here and in a new migration.
 */
public enum SettingKey {
    /** BR-12: water fee per month, part of the semester fee. */
    WATER_FEE_MONTHLY("water_fee_monthly", SettingType.MONEY),
    /** BR-12: equipment fee, charged once in FIRST_TIME rounds. */
    EQUIPMENT_FEE("equipment_fee", SettingType.MONEY),
    /** BR-07: electricity price per kWh. */
    ELECTRICITY_PRICE_PER_KWH("electricity_price_per_kwh", SettingType.MONEY),
    /** BR-05: hold time pre-filled for a new registration round. */
    DEFAULT_HOLD_MINUTES("default_hold_minutes", SettingType.MINUTES),
    /** BR-09: days after issue after which an unpaid electricity invoice blocks stay-on. */
    OVERDUE_DAYS_BLOCK_STAY_ON("overdue_days_block_stay_on", SettingType.DAYS),
    /** BR-10: repair deadline by priority. */
    REPAIR_DEADLINE_HOURS_URGENT("repair_deadline_hours_urgent", SettingType.HOURS),
    REPAIR_DEADLINE_HOURS_NORMAL("repair_deadline_hours_normal", SettingType.HOURS),
    REPAIR_DEADLINE_HOURS_LOW("repair_deadline_hours_low", SettingType.HOURS),
    /** BR-11: violations in one term that create a warning review. */
    WARNING_THRESHOLD("warning_threshold", SettingType.INTEGER);

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
