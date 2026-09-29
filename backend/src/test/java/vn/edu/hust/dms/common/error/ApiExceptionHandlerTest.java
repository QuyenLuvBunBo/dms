package vn.edu.hust.dms.common.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;
import vn.edu.hust.dms.support.TestUsers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercises the advice through the test-only ErrorProbeController over real HTTP. */
class ApiExceptionHandlerTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    private ApiClient api;

    @BeforeEach
    void logIn() {
        api = api();
        assertThat(api.loginAs(testUsers.create(Role.ADMIN)).status()).isEqualTo(200);
    }

    @Test
    @DisplayName("ERROR InvalidStateTransitionException maps to 409 application/problem+json")
    void invalidStateTransitionIsConflict() {
        ApiResponse response = api.get("/api/test/errors/conflict");

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("status").asInt()).isEqualTo(409);
        assertThat(response.body().path("title").asString()).isEqualTo("Conflict");
        assertThat(response.body().path("detail").asString()).isEqualTo("Cannot confirm a registration in status EXPIRED");
    }

    @Test
    @DisplayName("ERROR BusinessRuleViolationException maps to 422 and carries the rule id")
    void businessRuleViolationIsUnprocessable() {
        ApiResponse response = api.get("/api/test/errors/rule");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("rule").asString()).isEqualTo("BR-03");
        assertThat(response.body().path("detail").asString()).isEqualTo("Cannot hold a bed in a FEMALE room for a MALE student");
    }

    @Test
    @DisplayName("ERROR ConflictException maps to 409")
    void conflictIs409() {
        ApiResponse response = api.get("/api/test/errors/resource-conflict");

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("title").asString()).isEqualTo("Conflict");
        assertThat(response.body().path("detail").asString()).isEqualTo("Room 301 has occupied beds");
    }

    @Test
    @DisplayName("ERROR InvalidRequestException maps to 422 without a rule")
    void invalidRequestIsUnprocessableWithoutRule() {
        ApiResponse response = api.get("/api/test/errors/invalid-request");

        assertThat(response.status()).isEqualTo(422);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("detail").asString()).isEqualTo("The room type belongs to another building");
        assertThat(response.body().has("rule")).isFalse();
    }

    @Test
    @DisplayName("ERROR DataIntegrityViolationException maps to 409 without SQL details")
    void dataIntegrityViolationIs409WithoutSql() {
        ApiResponse response = api.get("/api/test/errors/data-integrity");

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("detail").asString()).isEqualTo("The change conflicts with existing data");
        assertThat(response.body().toString()).doesNotContain("Duplicate").doesNotContain("uk_buildings_code");
    }

    @Test
    @DisplayName("ERROR NotFoundException maps to 404")
    void notFoundIs404() {
        ApiResponse response = api.get("/api/test/errors/not-found");

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("detail").asString()).isEqualTo("Registration 42 not found");
    }

    @Test
    @DisplayName("ERROR @Valid failure maps to 400 with field errors")
    void validationFailureIs400WithFieldErrors() {
        ApiResponse response = api.post("/api/test/errors/validated", Map.of("name", " ", "quantity", 0));

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("errors").findValuesAsString("field"))
                .containsExactlyInAnyOrder("name", "quantity");

        ApiResponse ok = api.post("/api/test/errors/validated", Map.of("name", "Bed 3", "quantity", 2));
        assertThat(ok.status()).isEqualTo(200);
    }

    @Test
    @DisplayName("ERROR an unexpected exception maps to 500 without internals")
    void unexpectedExceptionIs500WithoutInternals() {
        ApiResponse response = api.get("/api/test/errors/unexpected");

        assertThat(response.status()).isEqualTo(500);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("detail").asString()).isEqualTo("Unexpected error");
        assertThat(response.body().toString()).doesNotContain("boom").doesNotContain("IllegalStateException");
    }
}
