package vn.edu.hust.dms.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.hust.dms.common.settings.SettingKey;
import vn.edu.hust.dms.common.settings.SettingsService;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;
import vn.edu.hust.dms.support.TestUsers;
import vn.edu.hust.dms.support.TestUsers.Credentials;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfProtectionTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private SettingsService settings;

    @Test
    @DisplayName("CSRF POST /api/auth/login without X-XSRF-TOKEN is rejected with 403")
    void loginWithoutHeaderIsRejected() {
        Credentials user = testUsers.create(Role.STUDENT);
        ApiClient api = api();
        api.get("/api/me");
        assertThat(api.csrfToken()).as("cookie present, header deliberately omitted").isPresent();

        ApiResponse response = api.postRaw("/api/auth/login",
                Map.of("username", user.username(), "password", user.password()), Map.of());

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("detail").asString()).isEqualTo("Invalid or missing CSRF token");
        assertThat(api.cookie(ApiClient.SESSION_COOKIE)).isEmpty();
        assertThat(api.get("/api/me").status()).isEqualTo(401);
    }

    @Test
    @DisplayName("CSRF POST with a header that does not match the cookie is rejected with 403 even with a valid session")
    void mismatchedHeaderIsRejected() {
        Credentials admin = testUsers.create(Role.ADMIN);
        ApiClient api = api();
        assertThat(api.loginAs(admin).status()).isEqualTo(200);

        ApiResponse response = api.putRaw("/api/settings/default_hold_minutes", Map.of("value", "24"),
                Map.of(ApiClient.XSRF_HEADER, "not-the-token"));

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.body().path("detail").asString()).isEqualTo("Invalid or missing CSRF token");
        assertThat(settings.getInt(SettingKey.DEFAULT_HOLD_MINUTES)).as("nothing changed").isEqualTo(30);
        assertThat(api.get("/api/me").status()).as("the session itself survives").isEqualTo(200);
    }

    @Test
    @DisplayName("CSRF GET never requires the token")
    void getNeverRequiresToken() {
        Credentials admin = testUsers.create(Role.ADMIN);
        ApiClient api = api();
        assertThat(api.loginAs(admin).status()).isEqualTo(200);
        api.removeCookie(ApiClient.XSRF_COOKIE);
        assertThat(api.csrfToken()).isEmpty();

        ApiResponse response = api.get("/api/settings");

        assertThat(response.status()).isEqualTo(200);
        assertThat(api.csrfToken()).as("a fresh token is issued with the response").isPresent();
    }
}
