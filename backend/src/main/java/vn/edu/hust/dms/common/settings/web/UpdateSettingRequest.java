package vn.edu.hust.dms.common.settings.web;

import jakarta.validation.constraints.NotBlank;

public record UpdateSettingRequest(@NotBlank String value) {
}
