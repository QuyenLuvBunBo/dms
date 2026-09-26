package vn.edu.hust.dms.common.settings;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import vn.edu.hust.dms.common.error.DomainException;
import vn.edu.hust.dms.common.error.NotFoundException;
import vn.edu.hust.dms.support.AbstractIntegrationTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SettingsServiceTest extends AbstractIntegrationTest {

    @Autowired
    private SettingsService settings;

    @Autowired
    private SettingRepository settingRepository;

    @Test
    @DisplayName("SETTINGS V2 seeds offer_hours=48, warning_threshold=3 and repair_due_hours urgent=24 normal=72 low=168")
    void seededDefaults() {
        assertThat(settings.getInt(SettingKey.OFFER_HOURS)).isEqualTo(48);
        assertThat(settings.getInt(SettingKey.WARNING_THRESHOLD)).isEqualTo(3);
        assertThat(settings.getInt(SettingKey.REPAIR_DUE_HOURS_URGENT)).isEqualTo(24);
        assertThat(settings.getInt(SettingKey.REPAIR_DUE_HOURS_NORMAL)).isEqualTo(72);
        assertThat(settings.getInt(SettingKey.REPAIR_DUE_HOURS_LOW)).isEqualTo(168);
        assertThat(settings.all()).extracting(Setting::getKey).containsExactly(
                "offer_hours", "repair_due_hours_low", "repair_due_hours_normal", "repair_due_hours_urgent",
                "warning_threshold");
        assertThat(settings.all()).allSatisfy(setting -> assertThat(setting.getDescription()).isNotBlank());
    }

    @Test
    @DisplayName("SETTINGS typed getters return int, long and Duration values and update() is visible to the next read")
    void typedGettersAndUpdate() {
        assertThat(settings.getLong(SettingKey.OFFER_HOURS)).isEqualTo(48L);
        assertThat(settings.getDuration(SettingKey.OFFER_HOURS)).isEqualTo(Duration.ofHours(48));
        assertThat(settings.getDuration(SettingKey.REPAIR_DUE_HOURS_LOW)).isEqualTo(Duration.ofDays(7));
        assertThatThrownBy(() -> settings.getDuration(SettingKey.WARNING_THRESHOLD))
                .isInstanceOf(IllegalArgumentException.class);

        clock.advance(Duration.ofMinutes(5));
        Setting updated = settings.update("offer_hours", " 72 ");

        assertThat(updated.getValue()).isEqualTo("72");
        assertThat(updated.getUpdatedAt()).isEqualTo(clock.instant());
        assertThat(settings.getInt(SettingKey.OFFER_HOURS)).isEqualTo(72);
        assertThat(settings.getDuration(SettingKey.OFFER_HOURS)).isEqualTo(Duration.ofHours(72));
    }

    @Test
    @DisplayName("SETTINGS update() with a value that does not parse for the key type throws InvalidSettingValueException (422)")
    void invalidValueIsRejected() {
        assertThatThrownBy(() -> settings.update("offer_hours", "two days"))
                .isInstanceOf(InvalidSettingValueException.class)
                .satisfies(ex -> assertThat(((DomainException) ex).status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY))
                .hasMessageContaining("offer_hours");
        assertThatThrownBy(() -> settings.update("offer_hours", "-1"))
                .isInstanceOf(InvalidSettingValueException.class);
        assertThatThrownBy(() -> settings.update("warning_threshold", "3.5"))
                .isInstanceOf(InvalidSettingValueException.class);
        assertThat(settings.getInt(SettingKey.OFFER_HOURS)).isEqualTo(48);
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
        settingRepository.deleteById("offer_hours");

        assertThatThrownBy(() -> settings.getInt(SettingKey.OFFER_HOURS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("offer_hours");
    }
}
