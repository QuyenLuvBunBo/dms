package vn.edu.hust.dms.common.settings.web;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.dms.common.settings.SettingsService;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
@PreAuthorize("hasRole('ADMIN')")
public class SettingsController {

    private final SettingsService settings;

    public SettingsController(SettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public List<SettingResponse> all() {
        return settings.all().stream().map(SettingResponse::from).toList();
    }

    @PutMapping("/{key}")
    public SettingResponse update(@PathVariable String key, @Valid @RequestBody UpdateSettingRequest body) {
        return SettingResponse.from(settings.update(key, body.value()));
    }
}
