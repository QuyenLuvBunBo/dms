package vn.edu.hust.dms.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.hust.dms.common.settings.SettingKey;
import vn.edu.hust.dms.common.settings.SettingsService;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;
import vn.edu.hust.dms.support.TestUsers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Role access against real endpoints: the settings API is the ADMIN-only surface of Phase 0. */
class RoleAccessTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private SettingsService settings;

    @ParameterizedTest(name = "ROLE {0} gets 403 problem+json on GET /api/settings")
    @EnumSource(value = Role.class, names = "ADMIN", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("ROLE non-admin roles get 403 problem+json on GET /api/settings")
    void nonAdminRolesAreForbidden(Role role) {
        ApiClient api = api();
        assertThat(api.loginAs(testUsers.create(role)).status()).isEqualTo(200);

        ApiResponse response = api.get("/api/settings");

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("status").asInt()).isEqualTo(403);
        assertThat(response.body().path("detail").asString()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("ROLE ADMIN gets 200 on GET /api/settings and PUT /api/settings/default_hold_minutes changes the value")
    void adminReadsAndUpdatesSettings() {
        ApiClient api = api();
        assertThat(api.loginAs(testUsers.create(Role.ADMIN)).status()).isEqualTo(200);

        ApiResponse list = api.get("/api/settings");
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body().size()).isEqualTo(9);
        assertThat(list.body().findValuesAsString("key"))
                .contains("default_hold_minutes", "warning_threshold", "repair_deadline_hours_urgent");

        ApiResponse update = api.put("/api/settings/default_hold_minutes", Map.of("value", "72"));
        assertThat(update.status()).isEqualTo(200);
        assertThat(update.body().path("key").asString()).isEqualTo("default_hold_minutes");
        assertThat(update.body().path("value").asString()).isEqualTo("72");
        assertThat(update.body().path("type").asString()).isEqualTo("MINUTES");
        assertThat(settings.getInt(SettingKey.DEFAULT_HOLD_MINUTES)).isEqualTo(72);

        ApiResponse invalid = api.put("/api/settings/default_hold_minutes", Map.of("value", "soon"));
        assertThat(invalid.status()).isEqualTo(422);
        ApiResponse unknown = api.put("/api/settings/no_such_key", Map.of("value", "1"));
        assertThat(unknown.status()).isEqualTo(404);
    }

    @Test
    @DisplayName("ROLE STUDENT PUT /api/settings/default_hold_minutes returns 403 and leaves the value unchanged")
    void studentCannotUpdateSettings() {
        ApiClient api = api();
        assertThat(api.loginAs(testUsers.create(Role.STUDENT)).status()).isEqualTo(200);

        ApiResponse response = api.put("/api/settings/default_hold_minutes", Map.of("value", "1"));

        assertThat(response.status()).isEqualTo(403);
        assertThat(settings.getInt(SettingKey.DEFAULT_HOLD_MINUTES)).isEqualTo(30);
    }
}
