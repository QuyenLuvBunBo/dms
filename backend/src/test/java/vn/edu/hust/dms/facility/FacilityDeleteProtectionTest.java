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
import vn.edu.hust.dms.support.FacilityFixture;
import vn.edu.hust.dms.support.FacilityFixture.Building;
import vn.edu.hust.dms.support.TestUsers;
import vn.edu.hust.dms.support.TestUsers.Credentials;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.edu.hust.dms.support.FacilityFixture.expect;
import static vn.edu.hust.dms.support.FacilityFixture.roomType;

/**
 * Facility data is deleted bottom-up, and never while a bed has an occupant. Occupants are marked
 * through TestBedOccupancy: in Phase 1 no table can place a student in a bed yet.
 */
class FacilityDeleteProtectionTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiClient admin;
    private FacilityFixture fixture;
    private Building b6;

    @BeforeEach
    void buildB6() {
        admin = loggedIn(testUsers.create(Role.ADMIN));
        fixture = new FacilityFixture(admin);
        b6 = fixture.building("B6");
    }

    @Test
    @DisplayName("DELETE a room with an occupied bed cannot be deleted (409) and keeps its beds and assets")
    void roomWithAnOccupantIsKept() {
        bedOccupancy.occupy(b6.bedIds().get(4));

        ApiResponse response = admin.delete("/api/rooms/" + b6.roomId());

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.contentType()).startsWith("application/problem+json");
        JsonNode room = expect(200, admin.get("/api/rooms/" + b6.roomId()));
        assertThat(room.path("beds").size()).isEqualTo(8);
        assertThat(room.path("assets").size()).isEqualTo(1);
    }

    @Test
    @DisplayName("DELETE a room without occupants is deleted together with its beds and assets")
    void emptyRoomIsDeletedWithItsBedsAndAssets() {
        expect(204, admin.delete("/api/rooms/" + b6.roomId()));

        assertThat(admin.get("/api/rooms/" + b6.roomId()).status()).isEqualTo(404);
        assertThat(count("SELECT COUNT(*) FROM beds WHERE room_id = ?", b6.roomId())).isZero();
        assertThat(count("SELECT COUNT(*) FROM room_assets WHERE room_id = ?", b6.roomId())).isZero();
    }

    @Test
    @DisplayName("DELETE an occupied bed cannot be removed (409) and stays; a free bed in the same room can still be removed")
    void occupiedBedIsKept() {
        long occupied = b6.bedIds().get(0);
        long free = b6.bedIds().get(1);
        bedOccupancy.occupy(occupied);

        ApiResponse refused = admin.delete("/api/beds/" + occupied);

        assertThat(refused.status()).isEqualTo(409);
        assertThat(count("SELECT COUNT(*) FROM beds WHERE id = ?", occupied)).isEqualTo(1L);
        expect(204, admin.delete("/api/beds/" + free));
        JsonNode room = expect(200, admin.get("/api/rooms/" + b6.roomId()));
        assertThat(room.path("beds").size()).isEqualTo(7);
        assertThat(room.path("beds").get(0).path("id").asLong()).isEqualTo(occupied);
        assertThat(room.path("beds").get(0).path("occupied").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("DELETE a floor with rooms cannot be deleted (409)")
    void floorWithRoomsIsKept() {
        assertThat(admin.delete("/api/floors/" + b6.floorId()).status()).isEqualTo(409);
        assertThat(count("SELECT COUNT(*) FROM floors WHERE id = ?", b6.floorId())).isEqualTo(1L);

        expect(204, admin.delete("/api/rooms/" + b6.roomId()));
        expect(204, admin.delete("/api/floors/" + b6.floorId()));
        assertThat(count("SELECT COUNT(*) FROM floors WHERE id = ?", b6.floorId())).isZero();
    }

    @Test
    @DisplayName("DELETE a room type used by a room cannot be deleted (409)")
    void roomTypeInUseIsKept() {
        assertThat(admin.delete("/api/room-types/" + b6.roomTypeId()).status()).isEqualTo(409);
        assertThat(count("SELECT COUNT(*) FROM room_types WHERE id = ?", b6.roomTypeId())).isEqualTo(1L);

        long unused = expect(201, admin.post("/api/buildings/" + b6.id() + "/room-types",
                roomType("10-student room", 10, 550_000))).path("id").asLong();
        expect(204, admin.delete("/api/room-types/" + unused));
    }

    @Test
    @DisplayName("DELETE a building with floors or room types cannot be deleted (409); an empty one can, and its manager links go with it")
    void buildingIsDeletedOnlyWhenEmpty() {
        assertThat(admin.delete("/api/buildings/" + b6.id()).status()).isEqualTo(409);

        long b3 = expect(201, admin.post("/api/buildings", Map.of("code", "B3", "name", "Building B3"))).path("id").asLong();
        long floor = expect(201, admin.post("/api/buildings/" + b3 + "/floors", Map.of("number", 1))).path("id").asLong();
        assertThat(admin.delete("/api/buildings/" + b3).status()).as("floor left").isEqualTo(409);
        expect(204, admin.delete("/api/floors/" + floor));
        long type = expect(201, admin.post("/api/buildings/" + b3 + "/room-types", roomType("10-student room", 10, 550_000)))
                .path("id").asLong();
        assertThat(admin.delete("/api/buildings/" + b3).status()).as("room type left").isEqualTo(409);
        expect(204, admin.delete("/api/room-types/" + type));
        Credentials manager = testUsers.create(Role.BUILDING_MANAGER);
        fixture.link(b3, manager.id());

        expect(204, admin.delete("/api/buildings/" + b3));

        assertThat(admin.get("/api/buildings/" + b3).status()).isEqualTo(404);
        assertThat(count("SELECT COUNT(*) FROM building_managers WHERE building_id = ?", b3)).isZero();
        assertThat(count("SELECT COUNT(*) FROM buildings WHERE id = ?", b6.id())).isEqualTo(1L);
    }

    private long count(String sql, long id) {
        Long count = jdbc.queryForObject(sql, Long.class, id);
        return count == null ? 0 : count;
    }
}
