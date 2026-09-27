package vn.edu.hust.dms.facility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;
import vn.edu.hust.dms.support.FacilityEndpoints;
import vn.edu.hust.dms.support.FacilityEndpoints.Endpoint;
import vn.edu.hust.dms.support.FacilityFixture;
import vn.edu.hust.dms.support.FacilityFixture.Building;
import vn.edu.hust.dms.support.TestUsers;
import vn.edu.hust.dms.support.TestUsers.Credentials;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Role checks on the facility API. Only ADMIN and BUILDING_MANAGER pass the role gate; for a
 * manager, the building scope (BR-14) is checked before the ADMIN-only check, so a 403 appears
 * only inside their own building.
 */
class FacilityAccessTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbc;

    private Building b6;
    private Building b9;
    private Credentials m6;

    @BeforeEach
    void buildTwoBuildings() {
        FacilityFixture fixture = new FacilityFixture(loggedIn(testUsers.create(Role.ADMIN)));
        b6 = fixture.building("B6");
        b9 = fixture.building("B9");
        m6 = testUsers.create(Role.BUILDING_MANAGER);
        fixture.link(b6.id(), m6.id());
    }

    @ParameterizedTest(name = "ACCESS the B6 manager gets 403 on ADMIN-only {0} in B6")
    @MethodSource("vn.edu.hust.dms.support.FacilityEndpoints#adminOnly")
    @DisplayName("ACCESS the B6 manager gets 403 on every ADMIN-only endpoint in B6")
    void adminOnlyEndpointsAreForbiddenInOwnBuilding(Endpoint endpoint) {
        List<String> before = FacilityFixture.snapshot(jdbc, b6.id());

        ApiResponse response = endpoint.call(loggedIn(m6), b6, m6.id());

        assertThat(response.status()).isEqualTo(403);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(response.body().path("detail").asString()).isEqualTo("Access denied");
        assertThat(FacilityFixture.snapshot(jdbc, b6.id())).isEqualTo(before);
    }

    @ParameterizedTest(name = "ACCESS {0} gets 403 on every facility endpoint, for B6, B9 and non-existent ids alike")
    @EnumSource(value = Role.class, names = {"STUDENT", "TECHNICIAN", "ACCOUNTANT"})
    @DisplayName("ACCESS STUDENT, TECHNICIAN and ACCOUNTANT get 403 on every facility endpoint, for B6, B9 and non-existent ids alike")
    void otherRolesAreForbiddenEverywhere(Role role) {
        Credentials user = testUsers.create(role);
        ApiClient api = loggedIn(user);
        List<String> b6Before = FacilityFixture.snapshot(jdbc, b6.id());
        List<String> b9Before = FacilityFixture.snapshot(jdbc, b9.id());

        for (Endpoint endpoint : FacilityEndpoints.all().toList()) {
            for (Building building : List.of(b6, b9, FacilityFixture.MISSING)) {
                assertThat(endpoint.call(api, building, user.id()).status())
                        .as(endpoint + " on " + building.code()).isEqualTo(403);
            }
        }
        assertThat(api.get("/api/buildings").status()).isEqualTo(403);
        assertThat(api.post("/api/buildings", Map.of("code", "B3", "name", "Building B3")).status()).isEqualTo(403);
        assertThat(api.get("/api/building-managers").status()).isEqualTo(403);
        assertThat(FacilityFixture.snapshot(jdbc, b6.id())).isEqualTo(b6Before);
        assertThat(FacilityFixture.snapshot(jdbc, b9.id())).isEqualTo(b9Before);
    }

    @Test
    @DisplayName("ACCESS a building manager gets 403 on POST /api/buildings and GET /api/building-managers")
    void managerCannotCreateBuildingsOrListManagers() {
        ApiClient api = loggedIn(m6);

        assertThat(api.post("/api/buildings", Map.of("code", "B3", "name", "Building B3")).status()).isEqualTo(403);
        assertThat(api.get("/api/building-managers").status()).isEqualTo(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM buildings", Long.class)).isEqualTo(2L);
    }

    @Test
    @DisplayName("ACCESS an anonymous request to a facility endpoint gets 401")
    void anonymousRequestsAreUnauthorized() {
        ApiClient anonymous = api();
        anonymous.get("/api/me");

        for (Endpoint endpoint : FacilityEndpoints.all().toList()) {
            assertThat(endpoint.call(anonymous, b6, m6.id()).status()).as(endpoint.toString()).isEqualTo(401);
        }
        assertThat(anonymous.get("/api/buildings").status()).isEqualTo(401);
        assertThat(anonymous.post("/api/buildings", Map.of("code", "B3", "name", "Building B3")).status())
                .isEqualTo(401);
        assertThat(anonymous.get("/api/building-managers").status()).isEqualTo(401);
    }
}
