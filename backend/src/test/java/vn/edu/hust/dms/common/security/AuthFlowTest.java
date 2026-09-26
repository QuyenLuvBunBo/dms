package vn.edu.hust.dms.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;
import vn.edu.hust.dms.support.TestUsers;
import vn.edu.hust.dms.support.TestUsers.Credentials;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFlowTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Test
    @DisplayName("AUTH anonymous GET /api/me returns 401 problem+json and sets a readable XSRF-TOKEN cookie")
    void anonymousMeIsUnauthorizedAndSetsCsrfCookie() {
        ApiClient api = api();

        ApiResponse response = api.get("/api/me");

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("status").asInt()).isEqualTo(401);
        assertThat(response.body().path("detail").asString()).isEqualTo("Authentication required");
        assertThat(api.csrfToken()).isPresent();
        assertThat(response.setCookies()).anySatisfy(cookie -> {
            assertThat(cookie).startsWith(ApiClient.XSRF_COOKIE + "=");
            assertThat(cookie).doesNotContainIgnoringCase("HttpOnly");
        });
        assertThat(api.cookie(ApiClient.SESSION_COOKIE)).as("no session for anonymous requests").isEmpty();
    }

    @ParameterizedTest(name = "AUTH login as {0} returns the user and GET /api/me reports the same role")
    @EnumSource(Role.class)
    @DisplayName("AUTH login as each role returns the user and GET /api/me reports the same role")
    void loginPerRole(Role role) {
        Credentials user = testUsers.create(role);
        ApiClient api = api();

        ApiResponse login = api.loginAs(user);

        assertThat(login.status()).isEqualTo(200);
        assertThat(login.body().path("id").asLong()).isEqualTo(user.id());
        assertThat(login.body().path("username").asString()).isEqualTo(user.username());
        assertThat(login.body().path("fullName").asString()).isEqualTo("Test " + role.name());
        assertThat(login.body().path("role").asString()).isEqualTo(role.name());

        ApiResponse me = api.get("/api/me");
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body().path("username").asString()).isEqualTo(user.username());
        assertThat(me.body().path("role").asString()).isEqualTo(role.name());
    }

    @Test
    @DisplayName("AUTH login with a wrong password returns 401 and no JSESSIONID")
    void wrongPassword() {
        Credentials user = testUsers.create(Role.STUDENT);
        ApiClient api = api();

        ApiResponse response = api.loginAs(user.username(), "not-the-password");

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("detail").asString()).isEqualTo("Invalid username or password");
        assertThat(api.cookie(ApiClient.SESSION_COOKIE)).isEmpty();
        assertThat(api.get("/api/me").status()).isEqualTo(401);
    }

    @Test
    @DisplayName("AUTH a disabled account cannot log in")
    void disabledAccount() {
        Credentials user = testUsers.create(Role.STUDENT, false);

        ApiResponse response = api().loginAs(user);

        assertThat(response.status()).isEqualTo(401);
        assertThat(response.body().path("detail").asString()).isEqualTo("Account is disabled");
    }

    @Test
    @DisplayName("AUTH login sets an HttpOnly SameSite=Lax JSESSIONID, changes the session id and rotates the XSRF-TOKEN cookie")
    void loginHardensSessionAndRotatesCsrfToken() {
        Credentials user = testUsers.create(Role.ADMIN);
        ApiClient api = api();
        api.get("/api/me");
        String tokenBeforeLogin = api.csrfToken().orElseThrow();

        ApiResponse firstLogin = api.loginAs(user);

        assertThat(firstLogin.status()).isEqualTo(200);
        assertThat(firstLogin.setCookies()).anySatisfy(cookie -> {
            assertThat(cookie).startsWith(ApiClient.SESSION_COOKIE + "=");
            assertThat(cookie).containsIgnoringCase("HttpOnly");
            assertThat(cookie).containsIgnoringCase("SameSite=Lax");
        });
        String sessionAfterFirstLogin = api.cookie(ApiClient.SESSION_COOKIE).orElseThrow();
        String tokenAfterFirstLogin = api.csrfToken().orElseThrow();
        assertThat(tokenAfterFirstLogin).as("CSRF token rotated at login").isNotEqualTo(tokenBeforeLogin);

        ApiResponse secondLogin = api.loginAs(user);

        assertThat(secondLogin.status()).isEqualTo(200);
        assertThat(api.cookie(ApiClient.SESSION_COOKIE).orElseThrow())
                .as("session id changes when logging in on an existing session")
                .isNotEqualTo(sessionAfterFirstLogin);
        assertThat(api.get("/api/me").status()).isEqualTo(200);
    }

    @Test
    @DisplayName("AUTH POST /api/auth/logout returns 204 and GET /api/me returns 401 afterwards")
    void logoutEndsSession() {
        Credentials user = testUsers.create(Role.ACCOUNTANT);
        ApiClient api = api();
        assertThat(api.loginAs(user).status()).isEqualTo(200);
        assertThat(api.get("/api/me").status()).isEqualTo(200);

        ApiResponse logout = api.post("/api/auth/logout", null);

        assertThat(logout.status()).isEqualTo(204);
        assertThat(api.cookie(ApiClient.SESSION_COOKIE)).isEmpty();
        assertThat(api.get("/api/me").status()).isEqualTo(401);
    }
}
