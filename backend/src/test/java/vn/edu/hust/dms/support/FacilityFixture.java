package vn.edu.hust.dms.support;

import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds facility data through the real API as ADMIN, the way the admin screens do, so the facility
 * tests depend only on the HTTP contract. One instance wraps one logged-in ADMIN client.
 */
public final class FacilityFixture {

    /** Ids of one fixture building: floor 3 (MALE), an 8-student room type, room 301 with 8 beds and one asset. */
    public record Building(long id, String code, long floorId, long roomTypeId, long roomId, List<Long> bedIds,
                           long assetId) {

        public long bedId() {
            return bedIds.getFirst();
        }
    }

    /** Ids that exist nowhere, for comparing a 404 on another building with a 404 on nothing. */
    public static final Building MISSING =
            new Building(999_999L, "none", 999_998L, 999_997L, 999_996L, List.of(999_995L), 999_994L);

    private final ApiClient admin;

    public FacilityFixture(ApiClient admin) {
        this.admin = admin;
    }

    public ApiClient admin() {
        return admin;
    }

    public Building building(String code) {
        long buildingId = expect(201, admin.post("/api/buildings", Map.of("code", code, "name", "Building " + code)))
                .path("id").asLong();
        long floorId = expect(201, admin.post("/api/buildings/" + buildingId + "/floors",
                Map.of("number", 3, "genderPreference", "MALE"))).path("id").asLong();
        long roomTypeId = expect(201, admin.post("/api/buildings/" + buildingId + "/room-types",
                roomType("8-student room", 8, 730_000))).path("id").asLong();
        JsonNode room = expect(201, admin.post("/api/floors/" + floorId + "/rooms",
                Map.of("code", "301", "roomTypeId", roomTypeId)));
        long roomId = room.path("id").asLong();
        List<Long> bedIds = new ArrayList<>();
        room.path("beds").forEach(bed -> bedIds.add(bed.path("id").asLong()));
        long assetId = expect(201, admin.post("/api/rooms/" + roomId + "/assets",
                Map.of("name", "Air conditioner", "status", "GOOD"))).path("id").asLong();
        return new Building(buildingId, code, floorId, roomTypeId, roomId, List.copyOf(bedIds), assetId);
    }

    public void link(long buildingId, long userId) {
        expect(204, admin.put("/api/buildings/" + buildingId + "/managers/" + userId, null));
    }

    public void unlink(long buildingId, long userId) {
        expect(204, admin.delete("/api/buildings/" + buildingId + "/managers/" + userId));
    }

    /** A valid room type body: 38 m2, air conditioning, water heater, one bathroom. */
    public static Map<String, Object> roomType(String name, int capacity, long monthlyRent) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("capacity", capacity);
        body.put("areaM2", 38.0);
        body.put("hasAirConditioning", true);
        body.put("hasWaterHeater", true);
        body.put("bathrooms", 1);
        body.put("monthlyRent", monthlyRent);
        return body;
    }

    /** Fails with the response body when the status is not the expected one. */
    public static JsonNode expect(int status, ApiResponse response) {
        if (response.status() != status) {
            throw new AssertionError("Expected HTTP " + status + " but got " + response.status() + ": " + response.body());
        }
        return response.body();
    }

    /** Every facility row of one building (and the audit rows of its assets), for "nothing changed" checks. */
    public static List<String> snapshot(JdbcTemplate jdbc, long buildingId) {
        String roomsOfBuilding = "SELECT r.id FROM rooms r JOIN floors f ON f.id = r.floor_id WHERE f.building_id = ?";
        List<String> rows = new ArrayList<>();
        rows.addAll(dump(jdbc, "buildings", "SELECT * FROM buildings WHERE id = ?", buildingId));
        rows.addAll(dump(jdbc, "floors", "SELECT * FROM floors WHERE building_id = ? ORDER BY id", buildingId));
        rows.addAll(dump(jdbc, "room_types", "SELECT * FROM room_types WHERE building_id = ? ORDER BY id", buildingId));
        rows.addAll(dump(jdbc, "rooms", "SELECT * FROM rooms WHERE id IN (" + roomsOfBuilding + ") ORDER BY id",
                buildingId));
        rows.addAll(dump(jdbc, "beds", "SELECT * FROM beds WHERE room_id IN (" + roomsOfBuilding + ") ORDER BY id",
                buildingId));
        rows.addAll(dump(jdbc, "room_assets",
                "SELECT * FROM room_assets WHERE room_id IN (" + roomsOfBuilding + ") ORDER BY id", buildingId));
        rows.addAll(dump(jdbc, "audit_logs", "SELECT * FROM audit_logs WHERE subject_type = 'ROOM_ASSET' AND subject_id IN "
                + "(SELECT a.id FROM room_assets a WHERE a.room_id IN (" + roomsOfBuilding + ")) ORDER BY id", buildingId));
        rows.addAll(dump(jdbc, "building_managers",
                "SELECT * FROM building_managers WHERE building_id = ? ORDER BY user_id", buildingId));
        return rows;
    }

    private static List<String> dump(JdbcTemplate jdbc, String table, String sql, long buildingId) {
        return jdbc.queryForList(sql, buildingId).stream().map(row -> table + " " + row).toList();
    }
}
