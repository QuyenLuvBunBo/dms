package vn.edu.hust.dms.facility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.edu.hust.dms.support.FacilityFixture.expect;

/**
 * BR-14: a building manager reads and changes only the buildings linked to them; every other
 * building answers 404, exactly like an id that does not exist.
 */
class BuildingScopeTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbc;

    private FacilityFixture fixture;
    private Building b6;
    private Building b9;
    private Credentials m6;
    private Credentials m69;
    private Credentials m0;

    @BeforeEach
    void buildTwoBuildings() {
        fixture = new FacilityFixture(loggedIn(testUsers.create(Role.ADMIN)));
        b6 = fixture.building("B6");
        b9 = fixture.building("B9");
        m6 = testUsers.create(Role.BUILDING_MANAGER);
        fixture.link(b6.id(), m6.id());
        Credentials m9 = testUsers.create(Role.BUILDING_MANAGER);
        fixture.link(b9.id(), m9.id());
        m69 = testUsers.create(Role.BUILDING_MANAGER);
        fixture.link(b6.id(), m69.id());
        fixture.link(b9.id(), m69.id());
        m0 = testUsers.create(Role.BUILDING_MANAGER);
    }

    @Test
    @DisplayName("BR-14 GET /api/buildings for the B6 manager returns only B6")
    void managerListsOnlyTheirBuilding() {
        ApiResponse response = loggedIn(m6).get("/api/buildings");

        assertThat(response.status()).isEqualTo(200);
        assertThat(codes(response)).containsExactly("B6");
    }

    @Test
    @DisplayName("BR-14 a manager linked to B6 and B9 sees both buildings and no other")
    void managerOfTwoBuildingsSeesBoth() {
        expect(201, fixture.admin().post("/api/buildings", Map.of("code", "B3", "name", "Building B3")));
        ApiClient api = loggedIn(m69);

        assertThat(codes(api.get("/api/buildings"))).containsExactly("B6", "B9");
        for (Building building : List.of(b6, b9)) {
            assertThat(api.get("/api/buildings/" + building.id()).status()).isEqualTo(200);
            assertThat(api.get("/api/rooms/" + building.roomId()).status()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("BR-14 a manager linked to no building sees an empty list and gets 404 for every building")
    void managerWithoutBuildingSeesNothing() {
        ApiClient api = loggedIn(m0);

        ApiResponse list = api.get("/api/buildings");

        assertThat(list.status()).isEqualTo(200);
        assertThat(list.body().size()).isZero();
        for (Endpoint endpoint : FacilityEndpoints.all().toList()) {
            for (Building building : List.of(b6, b9)) {
                assertThat(endpoint.call(api, building, m0.id()).status())
                        .as(endpoint + " on " + building.code()).isEqualTo(404);
            }
        }
    }

    @ParameterizedTest(name = "BR-14 the B6 manager gets 404 on {0} for a B9 resource and nothing in B9 changes")
    @MethodSource("vn.edu.hust.dms.support.FacilityEndpoints#all")
    @DisplayName("BR-14 the B6 manager gets 404 on every facility endpoint for a B9 resource and nothing in B9 changes")
    void otherBuildingIsNotFound(Endpoint endpoint) {
        List<String> before = FacilityFixture.snapshot(jdbc, b9.id());

        ApiResponse response = endpoint.call(loggedIn(m6), b9, m6.id());

        assertThat(response.status()).isEqualTo(404);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(FacilityFixture.snapshot(jdbc, b9.id())).isEqualTo(before);
    }

    @ParameterizedTest(name = "BR-14 the 404 on {0} for a B9 resource is identical to the 404 for an id that does not exist")
    @MethodSource("vn.edu.hust.dms.support.FacilityEndpoints#all")
    @DisplayName("BR-14 on every facility endpoint the 404 for a B9 resource is identical to the 404 for an id that does not exist")
    void notFoundDoesNotRevealExistence(Endpoint endpoint) {
        ApiClient api = loggedIn(m6);

        ApiResponse foreign = endpoint.call(api, b9, m6.id());
        ApiResponse missing = endpoint.call(api, FacilityFixture.MISSING, m6.id());

        assertThat(foreign.status()).isEqualTo(404);
        assertThat(problem(foreign)).isEqualTo(problem(missing));
    }

    @ParameterizedTest(name = "BR-14 the B6 manager reaches {0} for the matching B6 resource")
    @MethodSource("vn.edu.hust.dms.support.FacilityEndpoints#managerAllowed")
    @DisplayName("BR-14 the B6 manager reaches every manager endpoint for the matching B6 resource")
    void ownBuildingIsReachable(Endpoint endpoint) {
        ApiResponse response = endpoint.call(loggedIn(m6), b6, m6.id());

        assertThat(response.status()).as(response.body().toString()).isBetween(200, 299);
    }

    @Test
    @DisplayName("BR-14 unassigning the B6 manager turns every B6 endpoint into 404 on the next request")
    void unassignmentTakesEffectOnTheNextRequest() {
        ApiClient api = loggedIn(m6);
        assertThat(api.get("/api/rooms/" + b6.roomId()).status()).isEqualTo(200);

        fixture.unlink(b6.id(), m6.id());

        for (Endpoint endpoint : FacilityEndpoints.all().toList()) {
            assertThat(endpoint.call(api, b6, m6.id()).status()).as(endpoint.toString()).isEqualTo(404);
        }
        assertThat(api.get("/api/buildings").body().size()).isZero();
    }

    @Test
    @DisplayName("BR-14 ADMIN sees every building in GET /api/buildings")
    void adminSeesEveryBuilding() {
        assertThat(codes(fixture.admin().get("/api/buildings"))).containsExactly("B6", "B9");
    }

    @ParameterizedTest(name = "BR-14 ADMIN is never refused by scope on {0} in B6 or B9")
    @MethodSource("vn.edu.hust.dms.support.FacilityEndpoints#all")
    @DisplayName("BR-14 ADMIN is never refused by scope (no 403 or 404) on any facility endpoint in B6 or B9")
    void adminIsNeverRefusedByScope(Endpoint endpoint) {
        for (Building building : List.of(b6, b9)) {
            ApiResponse response = endpoint.call(fixture.admin(), building, m6.id());

            assertThat(response.status()).as(endpoint + " on " + building.code() + ": " + response.body())
                    .isNotIn(403, 404);
        }
    }

    private record Problem(int status, String title, String detail) {
    }

    private static Problem problem(ApiResponse response) {
        return new Problem(response.body().path("status").asInt(), response.body().path("title").asString(),
                response.body().path("detail").asString());
    }

    private static List<String> codes(ApiResponse response) {
        List<String> codes = new ArrayList<>();
        response.body().forEach(building -> codes.add(building.path("code").asString()));
        return codes;
    }
}
