package vn.edu.hust.dms.facility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;
import vn.edu.hust.dms.support.TestUsers;
import vn.edu.hust.dms.support.TestUsers.Credentials;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.edu.hust.dms.support.FacilityFixture.expect;

/** The building_managers link, managed by ADMIN. */
class BuildingManagerAssignmentTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiClient admin;
    private long b6;
    private long b9;

    @BeforeEach
    void createBuildings() {
        admin = loggedIn(testUsers.create(Role.ADMIN));
        b6 = expect(201, admin.post("/api/buildings", Map.of("code", "B6", "name", "Building B6"))).path("id").asLong();
        b9 = expect(201, admin.post("/api/buildings", Map.of("code", "B9", "name", "Building B9"))).path("id").asLong();
    }

    @Test
    @DisplayName("MANAGERS ADMIN assigns a building manager to a building and unassigns them")
    void assignAndUnassign() {
        Credentials manager = testUsers.create(Role.BUILDING_MANAGER);
        ApiClient managerApi = loggedIn(manager);
        assertThat(managerApi.get("/api/buildings").body().size()).isZero();

        expect(204, admin.put(managerPath(b6, manager), null));

        JsonNode building = expect(200, admin.get("/api/buildings/" + b6));
        assertThat(building.path("managers").size()).isEqualTo(1);
        assertThat(building.path("managers").get(0).path("userId").asLong()).isEqualTo(manager.id());
        assertThat(building.path("managers").get(0).path("username").asString()).isEqualTo(manager.username());
        assertThat(codes(managerApi.get("/api/buildings").body())).containsExactly("B6");

        expect(204, admin.delete(managerPath(b6, manager)));

        assertThat(expect(200, admin.get("/api/buildings/" + b6)).path("managers").size()).isZero();
        assertThat(managerApi.get("/api/buildings").body().size()).isZero();
        assertThat(admin.delete(managerPath(b6, manager)).status()).as("unassigning twice is fine").isEqualTo(204);
    }

    @Test
    @DisplayName("MANAGERS a manager can be linked to more than one building, and assigning twice is idempotent")
    void severalBuildingsAndIdempotentAssignment() {
        Credentials manager = testUsers.create(Role.BUILDING_MANAGER);
        Credentials second = testUsers.create(Role.BUILDING_MANAGER);

        expect(204, admin.put(managerPath(b6, manager), null));
        expect(204, admin.put(managerPath(b9, manager), null));
        expect(204, admin.put(managerPath(b6, manager), null));
        expect(204, admin.put(managerPath(b6, second), null));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM building_managers WHERE user_id = ?", Long.class,
                manager.id())).isEqualTo(2L);
        assertThat(codes(loggedIn(manager).get("/api/buildings").body())).containsExactly("B6", "B9");
        assertThat(expect(200, admin.get("/api/buildings/" + b6)).path("managers").size())
                .as("a building may have several managers").isEqualTo(2);
    }

    @Test
    @DisplayName("MANAGERS only a user with role BUILDING_MANAGER can be assigned (422); an unknown user is 404")
    void onlyBuildingManagersCanBeAssigned() {
        for (Role role : List.of(Role.STUDENT, Role.ADMIN, Role.TECHNICIAN, Role.ACCOUNTANT)) {
            Credentials user = testUsers.create(role);

            ApiResponse response = admin.put(managerPath(b6, user), null);

            assertThat(response.status()).as(role.name()).isEqualTo(422);
        }
        ApiResponse unknown = admin.put("/api/buildings/" + b6 + "/managers/999999", null);

        assertThat(unknown.status()).isEqualTo(404);
        assertThat(unknown.body().path("detail").asString()).isEqualTo("User not found");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM building_managers", Long.class)).isZero();
    }

    @Test
    @DisplayName("MANAGERS GET /api/building-managers lists every building manager with their buildings")
    void listBuildingManagers() {
        Credentials linked = testUsers.create(Role.BUILDING_MANAGER);
        Credentials unlinked = testUsers.create(Role.BUILDING_MANAGER);
        testUsers.create(Role.STUDENT);
        expect(204, admin.put(managerPath(b9, linked), null));
        expect(204, admin.put(managerPath(b6, linked), null));

        JsonNode managers = expect(200, admin.get("/api/building-managers"));

        assertThat(managers.size()).isEqualTo(2);
        for (JsonNode manager : managers) {
            List<String> buildings = codes(manager.path("buildings"));
            if (manager.path("userId").asLong() == linked.id()) {
                assertThat(buildings).containsExactly("B6", "B9");
            } else {
                assertThat(manager.path("userId").asLong()).isEqualTo(unlinked.id());
                assertThat(buildings).isEmpty();
            }
        }
    }

    private static String managerPath(long buildingId, Credentials user) {
        return "/api/buildings/" + buildingId + "/managers/" + user.id();
    }

    private static List<String> codes(JsonNode buildings) {
        List<String> codes = new ArrayList<>();
        buildings.forEach(building -> codes.add(building.path("code").asString()));
        return codes;
    }
}
