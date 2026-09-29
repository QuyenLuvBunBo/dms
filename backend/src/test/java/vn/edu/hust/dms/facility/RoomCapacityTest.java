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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.edu.hust.dms.support.FacilityFixture.expect;
import static vn.edu.hust.dms.support.FacilityFixture.roomType;

/** A room starts with exactly as many beds as its room type's capacity and never gets more. */
class RoomCapacityTest extends AbstractIntegrationTest {

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
    @DisplayName("CAPACITY creating a room creates exactly as many beds as its room type's capacity, coded 1 to N")
    void newRoomGetsCapacityBeds() {
        long tenStudent = createRoomType(b6, "10-student room", 10);

        JsonNode created = expect(201, admin.post("/api/floors/" + b6.floorId() + "/rooms",
                Map.of("code", "302", "roomTypeId", tenStudent)));

        assertThat(bedCodes(created)).containsExactlyElementsOf(codes(1, 10));
        assertThat(bedCodes(room(created.path("id").asLong()))).containsExactlyElementsOf(codes(1, 10));
        assertThat(bedCodes(room(b6.roomId()))).containsExactlyElementsOf(codes(1, 8));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM beds WHERE room_id = ?", Long.class,
                created.path("id").asLong())).isEqualTo(10L);
    }

    @Test
    @DisplayName("CAPACITY a bed cannot be added to a room that already has as many beds as its capacity (409)")
    void fullRoomRejectsAnotherBed() {
        ApiResponse response = admin.post("/api/rooms/" + b6.roomId() + "/beds", Map.of());

        assertThat(response.status()).isEqualTo(409);
        assertThat(response.contentType()).startsWith("application/problem+json");
        assertThat(bedCodes(room(b6.roomId()))).hasSize(8);
    }

    @Test
    @DisplayName("CAPACITY after a bed is removed, one can be added back up to the capacity and gets the lowest free code")
    void removedBedCanBeAddedBack() {
        expect(204, admin.delete("/api/beds/" + bedId(b6.roomId(), "3")));
        assertThat(bedCodes(room(b6.roomId()))).containsExactly("1", "2", "4", "5", "6", "7", "8");

        JsonNode added = expect(201, admin.post("/api/rooms/" + b6.roomId() + "/beds", Map.of()));

        assertThat(added.path("code").asString()).isEqualTo("3");
        assertThat(added.path("occupied").asBoolean()).isFalse();
        assertThat(bedCodes(room(b6.roomId()))).containsExactlyElementsOf(codes(1, 8));
        assertThat(admin.post("/api/rooms/" + b6.roomId() + "/beds", Map.of()).status()).isEqualTo(409);
    }

    @Test
    @DisplayName("CAPACITY bed codes are unique within a room (409)")
    void bedCodesAreUniqueWithinARoom() {
        expect(204, admin.delete("/api/beds/" + bedId(b6.roomId(), "8")));

        assertThat(admin.post("/api/rooms/" + b6.roomId() + "/beds", Map.of("code", "1")).status()).isEqualTo(409);
        assertThat(admin.put("/api/beds/" + bedId(b6.roomId(), "2"), Map.of("code", "1")).status()).isEqualTo(409);
        assertThat(bedCodes(room(b6.roomId()))).hasSize(7);

        JsonNode relabelled = expect(200, admin.put("/api/beds/" + bedId(b6.roomId(), "2"), Map.of("code", "2A")));
        assertThat(relabelled.path("code").asString()).isEqualTo("2A");
        JsonNode explicit = expect(201, admin.post("/api/rooms/" + b6.roomId() + "/beds", Map.of("code", "8B")));
        assertThat(explicit.path("code").asString()).isEqualTo("8B");
        JsonNode otherRoom = expect(201, admin.post("/api/floors/" + b6.floorId() + "/rooms",
                Map.of("code", "302", "roomTypeId", b6.roomTypeId())));
        assertThat(bedCodes(otherRoom)).as("the same codes in another room are fine").contains("1", "2");
    }

    @Test
    @DisplayName("CAPACITY a room type's capacity cannot drop below the bed count of a room that uses it (409); raising it adds no beds")
    void capacityCannotDropBelowBedCount() {
        ApiResponse lowered = admin.put("/api/room-types/" + b6.roomTypeId(), roomType("8-student room", 6, 730_000));

        assertThat(lowered.status()).isEqualTo(409);
        assertThat(roomTypeCapacity(b6.roomTypeId())).isEqualTo(8);

        JsonNode raised = expect(200, admin.put("/api/room-types/" + b6.roomTypeId(),
                roomType("8-student room", 10, 730_000)));
        assertThat(raised.path("capacity").asInt()).isEqualTo(10);
        assertThat(bedCodes(room(b6.roomId()))).hasSize(8);

        expect(204, admin.delete("/api/beds/" + bedId(b6.roomId(), "7")));
        expect(204, admin.delete("/api/beds/" + bedId(b6.roomId(), "8")));
        JsonNode fits = expect(200, admin.put("/api/room-types/" + b6.roomTypeId(),
                roomType("8-student room", 6, 730_000)));
        assertThat(fits.path("capacity").asInt()).as("6 beds left, so capacity 6 fits").isEqualTo(6);
    }

    @Test
    @DisplayName("CAPACITY a room cannot switch to a room type whose capacity is below its bed count (409)")
    void roomCannotSwitchToASmallerRoomType() {
        long sixStudent = createRoomType(b6, "6-student room", 6);

        ApiResponse smaller = admin.put("/api/rooms/" + b6.roomId(), Map.of("code", "301", "roomTypeId", sixStudent));

        assertThat(smaller.status()).isEqualTo(409);
        assertThat(room(b6.roomId()).path("roomType").path("id").asLong()).isEqualTo(b6.roomTypeId());

        long tenStudent = createRoomType(b6, "10-student room", 10);
        JsonNode switched = expect(200, admin.put("/api/rooms/" + b6.roomId(),
                Map.of("code", "301", "roomTypeId", tenStudent)));
        assertThat(switched.path("roomType").path("id").asLong()).isEqualTo(tenStudent);
        assertThat(bedCodes(switched)).as("switching adds no beds").hasSize(8);
    }

    @Test
    @DisplayName("CAPACITY a room can only use a room type of its own building (422)")
    void roomTypeMustBelongToTheRoomsBuilding() {
        Building b9 = fixture.building("B9");

        ApiResponse created = admin.post("/api/floors/" + b6.floorId() + "/rooms",
                Map.of("code", "302", "roomTypeId", b9.roomTypeId()));
        ApiResponse updated = admin.put("/api/rooms/" + b6.roomId(),
                Map.of("code", "301", "roomTypeId", b9.roomTypeId()));

        assertThat(created.status()).isEqualTo(422);
        assertThat(updated.status()).isEqualTo(422);
        assertThat(updated.body().has("rule")).as("not a BR-xx rule").isFalse();
        assertThat(room(b6.roomId()).path("roomType").path("id").asLong()).isEqualTo(b6.roomTypeId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rooms WHERE floor_id = ?", Long.class, b6.floorId()))
                .isEqualTo(1L);
    }

    private long createRoomType(Building building, String name, int capacity) {
        return expect(201, admin.post("/api/buildings/" + building.id() + "/room-types",
                roomType(name, capacity, 500_000))).path("id").asLong();
    }

    private JsonNode room(long roomId) {
        return expect(200, admin.get("/api/rooms/" + roomId));
    }

    private int roomTypeCapacity(long roomTypeId) {
        for (JsonNode type : expect(200, admin.get("/api/buildings/" + b6.id() + "/room-types"))) {
            if (type.path("id").asLong() == roomTypeId) {
                return type.path("capacity").asInt();
            }
        }
        throw new AssertionError("room type " + roomTypeId + " not listed");
    }

    private long bedId(long roomId, String code) {
        for (JsonNode bed : room(roomId).path("beds")) {
            if (bed.path("code").asString().equals(code)) {
                return bed.path("id").asLong();
            }
        }
        throw new AssertionError("no bed " + code + " in room " + roomId);
    }

    private static List<String> bedCodes(JsonNode room) {
        List<String> codes = new ArrayList<>();
        room.path("beds").forEach(bed -> codes.add(bed.path("code").asString()));
        return codes;
    }

    private static List<String> codes(int from, int to) {
        return IntStream.rangeClosed(from, to).mapToObj(String::valueOf).toList();
    }
}
