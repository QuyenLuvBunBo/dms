package vn.edu.hust.dms.common.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import vn.edu.hust.dms.common.error.DomainException;
import vn.edu.hust.dms.common.error.NotFoundException;
import vn.edu.hust.dms.support.AbstractIntegrationTest;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SettingsService settings;

    @Autowired
    private SettingRepository settingRepository;

    @Test
    @DisplayName("SETTINGS V3 seeds the nine CLAUDE.md keys with their defaults")
    void seededDefaults() {
        assertThat(settings.getLong(SettingKey.WATER_FEE_MONTHLY)).isEqualTo(40_000L);
        assertThat(settings.getLong(SettingKey.EQUIPMENT_FEE)).isEqualTo(300_000L);
        assertThat(settings.getLong(SettingKey.ELECTRICITY_PRICE_PER_KWH)).isEqualTo(3_000L);
        assertThat(settings.getInt(SettingKey.DEFAULT_HOLD_MINUTES)).isEqualTo(30);
        assertThat(settings.getInt(SettingKey.OVERDUE_DAYS_BLOCK_STAY_ON)).isEqualTo(30);
        assertThat(settings.getInt(SettingKey.REPAIR_DEADLINE_HOURS_URGENT)).isEqualTo(24);
        assertThat(settings.getInt(SettingKey.REPAIR_DEADLINE_HOURS_NORMAL)).isEqualTo(72);
        assertThat(settings.getInt(SettingKey.REPAIR_DEADLINE_HOURS_LOW)).isEqualTo(168);
        assertThat(settings.getInt(SettingKey.WARNING_THRESHOLD)).isEqualTo(3);
        assertThat(settings.all()).extracting(Setting::getKey).containsExactly(
                "default_hold_minutes", "electricity_price_per_kwh", "equipment_fee", "overdue_days_block_stay_on",
                "repair_deadline_hours_low", "repair_deadline_hours_normal", "repair_deadline_hours_urgent",
                "warning_threshold", "water_fee_monthly");
        assertThat(settings.all()).allSatisfy(setting -> assertThat(setting.getDescription()).isNotBlank());
        assertThat(settingRepository.findAllById(List.of("offer_hours", "repair_due_hours_urgent",
                "repair_due_hours_normal", "repair_due_hours_low"))).as("the Phase 0 keys are gone").isEmpty();
    }

    @Test
    @DisplayName("SETTINGS typed getters return int, long and Duration values (minutes, hours, days) and update() is visible to the next read")
    void typedGettersAndUpdate() {
        assertThat(settings.getLong(SettingKey.EQUIPMENT_FEE)).isEqualTo(300_000L);
        assertThat(settings.getDuration(SettingKey.DEFAULT_HOLD_MINUTES)).isEqualTo(Duration.ofMinutes(30));
        assertThat(settings.getDuration(SettingKey.REPAIR_DEADLINE_HOURS_LOW)).isEqualTo(Duration.ofDays(7));
        assertThat(settings.getDuration(SettingKey.OVERDUE_DAYS_BLOCK_STAY_ON)).isEqualTo(Duration.ofDays(30));
        assertThatThrownBy(() -> settings.getDuration(SettingKey.WARNING_THRESHOLD))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> settings.getDuration(SettingKey.WATER_FEE_MONTHLY))
                .isInstanceOf(IllegalArgumentException.class);

        clock.advance(Duration.ofMinutes(5));
        Setting updated = settings.update("default_hold_minutes", " 45 ");

        assertThat(updated.getValue()).isEqualTo("45");
        assertThat(updated.getUpdatedAt()).isEqualTo(clock.instant());
        assertThat(settings.getInt(SettingKey.DEFAULT_HOLD_MINUTES)).isEqualTo(45);
        assertThat(settings.getDuration(SettingKey.DEFAULT_HOLD_MINUTES)).isEqualTo(Duration.ofMinutes(45));
    }

    @Test
    @DisplayName("SETTINGS update() with a value that does not parse for the key type throws InvalidSettingValueException (422)")
    void invalidValueIsRejected() {
        assertThatThrownBy(() -> settings.update("default_hold_minutes", "two days"))
                .isInstanceOf(InvalidSettingValueException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT))
                .hasMessageContaining("default_hold_minutes");
        assertThatThrownBy(() -> settings.update("default_hold_minutes", "0"))
                .isInstanceOf(InvalidSettingValueException.class);
        assertThatThrownBy(() -> settings.update("water_fee_monthly", "-1"))
                .isInstanceOf(InvalidSettingValueException.class);
        assertThatThrownBy(() -> settings.update("repair_deadline_hours_low", "-1"))
                .isInstanceOf(InvalidSettingValueException.class);
        assertThatThrownBy(() -> settings.update("overdue_days_block_stay_on", "-1"))
                .isInstanceOf(InvalidSettingValueException.class);
        assertThatThrownBy(() -> settings.update("warning_threshold", "3.5"))
                .isInstanceOf(InvalidSettingValueException.class);
        assertThat(settings.getInt(SettingKey.DEFAULT_HOLD_MINUTES)).isEqualTo(30);
        assertThat(settings.getLong(SettingKey.WATER_FEE_MONTHLY)).isEqualTo(40_000L);
    }

    @Test
    @DisplayName("SETTINGS update() of an unknown key throws NotFoundException (404)")
    void unknownKeyIsNotFound() {
        assertThatThrownBy(() -> settings.update("no_such_key", "1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("no_such_key");
    }

    @Test
    @DisplayName("SETTINGS reading a key whose row was deleted throws IllegalStateException (configuration error)")
    void missingRowIsConfigurationError() {
        settingRepository.deleteById("default_hold_minutes");

        assertThatThrownBy(() -> settings.getInt(SettingKey.DEFAULT_HOLD_MINUTES))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default_hold_minutes");
    }
}
