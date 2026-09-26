package vn.edu.hust.dms.common.settings.web;

import vn.edu.hust.dms.common.settings.Setting;
import vn.edu.hust.dms.common.settings.SettingKey;

import java.time.Instant;

public record SettingResponse(String key, String value, String type, String description, Instant updatedAt) {

    public static SettingResponse from(Setting setting) {
        String type = SettingKey.fromKey(setting.getKey()).map(key -> key.type().name()).orElse(null);
        return new SettingResponse(setting.getKey(), setting.getValue(), type, setting.getDescription(),
                setting.getUpdatedAt());
    }
}
