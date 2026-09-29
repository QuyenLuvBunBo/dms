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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static vn.edu.hust.dms.support.FacilityFixture.expect;
import static vn.edu.hust.dms.support.FacilityFixture.roomType;

/** The ADMIN facility screens: buildings, floors, room types and rooms through the API. */
class FacilityCrudTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private JdbcTemplate jdbc;

    private ApiClient admin;
    private FacilityFixture fixture;

    @BeforeEach
    void logInAsAdmin() {
        admin = loggedIn(testUsers.create(Role.ADMIN));
        fixture = new FacilityFixture(admin);
    }

    @Test
    @DisplayName("FACILITY ADMIN creates, renames and lists buildings; a duplicate code is rejected (409)")
    void buildings() {
        JsonNode b6 = expect(201, admin.post("/api/buildings", Map.of("code", "B6", "name", "Building B6")));
        expect(201, admin.post("/api/buildings", Map.of("code", "B10", "name", "Building B10")));
        long b9 = expect(201, admin.post("/api/buildings", Map.of("code", "B9", "name", "Building B9"))).path("id").asLong();

        assertThat(b6.path("code").asString()).isEqualTo("B6");
        assertThat(b6.path("floorCount").asInt()).isZero();
        assertThat(b6.path("managers").size()).isZero();
        JsonNode renamed = expect(200, admin.put("/api/buildings/" + b6.path("id").asLong(),
                Map.of("code", "B6", "name", "Nhà B6")));
        assertThat(renamed.path("name").asString()).isEqualTo("Nhà B6");
        assertThat(codes(expect(200, admin.get("/api/buildings")))).as("natural order")
                .containsExactly("B6", "B9", "B10");

        assertThat(admin.post("/api/buildings", Map.of("code", "B6", "name", "Again")).status()).isEqualTo(409);
        assertThat(admin.post("/api/buildings", Map.of("code", "b6", "name", "Again")).status())
                .as("codes are compared case-insensitively").isEqualTo(409);
        assertThat(admin.put("/api/buildings/" + b9, Map.of("code", "B6", "name", "Building B9")).status())
                .isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM buildings", Long.class)).isEqualTo(3L);
    }

    @Test
    @DisplayName("FACILITY floors carry an optional gender preference; floor numbers are unique per building (409)")
    void floors() {
        long b6 = createBuilding("B6");
        JsonNode first = expect(201, admin.post("/api/buildings/" + b6 + "/floors",
                Map.of("number", 1, "genderPreference", "MALE")));
        JsonNode second = expect(201, admin.post("/api/buildings/" + b6 + "/floors", Map.of("number", 2)));

        assertThat(first.path("genderPreference").asString()).isEqualTo("MALE");
        assertThat(second.path("genderPreference").isNull()).isTrue();
        JsonNode female = expect(200, admin.put("/api/floors/" + second.path("id").asLong(),
                Map.of("number", 2, "genderPreference", "FEMALE")));
        assertThat(female.path("genderPreference").asString()).isEqualTo("FEMALE");
        Map<String, Object> noPreference = new HashMap<>();
        noPreference.put("number", 1);
        noPreference.put("genderPreference", null);
        JsonNode cleared = expect(200, admin.put("/api/floors/" + first.path("id").asLong(), noPreference));
        assertThat(cleared.path("genderPreference").isNull()).isTrue();

        List<Integer> numbers = new ArrayList<>();
        expect(200, admin.get("/api/buildings/" + b6 + "/floors")).forEach(floor -> numbers.add(floor.path("number").asInt()));
        assertThat(numbers).containsExactly(1, 2);
        assertThat(admin.post("/api/buildings/" + b6 + "/floors", Map.of("number", 1)).status()).isEqualTo(409);
        assertThat(admin.put("/api/floors/" + second.path("id").asLong(), Map.of("number", 1)).status()).isEqualTo(409);
        long b9 = createBuilding("B9");
        expect(201, admin.post("/api/buildings/" + b9 + "/floors", Map.of("number", 1)));
    }

    @Test
    @DisplayName("FACILITY room types store capacity, area, air conditioning, water heater, bathrooms and monthly rent in VND")
    void roomTypes() {
        long b10 = createBuilding("B10");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "12-student room");
        body.put("capacity", 12);
        body.put("areaM2", 60.0);
        body.put("hasAirConditioning", false);
        body.put("hasWaterHeater", true);
        body.put("bathrooms", 2);
        body.put("monthlyRent", 320_000);

        expect(201, admin.post("/api/buildings/" + b10 + "/room-types", body));

        JsonNode listed = expect(200, admin.get("/api/buildings/" + b10 + "/room-types")).get(0);
        assertThat(listed.path("buildingId").asLong()).isEqualTo(b10);
        assertThat(listed.path("name").asString()).isEqualTo("12-student room");
        assertThat(listed.path("capacity").asInt()).isEqualTo(12);
        assertThat(listed.path("areaM2").decimalValue()).isEqualByComparingTo(new BigDecimal("60"));
        assertThat(listed.path("hasAirConditioning").asBoolean()).isFalse();
        assertThat(listed.path("hasWaterHeater").asBoolean()).isTrue();
        assertThat(listed.path("bathrooms").asInt()).isEqualTo(2);
        assertThat(listed.path("monthlyRent").asLong()).isEqualTo(320_000L);
        assertThat(listed.path("roomCount").asInt()).isZero();
        assertThat(admin.post("/api/buildings/" + b10 + "/room-types", body).status())
                .as("names are unique per building").isEqualTo(409);
    }

    @Test
    @DisplayName("FACILITY room codes are unique within a building, across floors (409)")
    void roomCodesAreUniquePerBuilding() {
        Building b6 = fixture.building("B6");
        fixture.building("B9");
        long floor4 = expect(201, admin.post("/api/buildings/" + b6.id() + "/floors", Map.of("number", 4)))
                .path("id").asLong();

        ApiResponse duplicate = admin.post("/api/floors/" + floor4 + "/rooms",
                Map.of("code", "301", "roomTypeId", b6.roomTypeId()));
        JsonNode room401 = expect(201, admin.post("/api/floors/" + floor4 + "/rooms",
                Map.of("code", "401", "roomTypeId", b6.roomTypeId())));
        ApiResponse renamedToDuplicate = admin.put("/api/rooms/" + room401.path("id").asLong(),
                Map.of("code", "301", "roomTypeId", b6.roomTypeId()));

        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(renamedToDuplicate.status()).isEqualTo(409);
        assertThat(room(room401.path("id").asLong()).path("code").asString()).isEqualTo("401");
    }

    @Test
    @DisplayName("FACILITY a new room has gender null and the API cannot set it")
    void roomGenderIsReadOnly() {
        Building b6 = fixture.building("B6");

        JsonNode created = expect(201, admin.post("/api/floors/" + b6.floorId() + "/rooms",
                Map.of("code", "302", "roomTypeId", b6.roomTypeId(), "gender", "FEMALE")));
        JsonNode updated = expect(200, admin.put("/api/rooms/" + created.path("id").asLong(),
                Map.of("code", "302", "roomTypeId", b6.roomTypeId(), "gender", "MALE")));

        assertThat(created.path("gender").isNull()).isTrue();
        assertThat(updated.path("gender").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rooms WHERE gender IS NOT NULL", Long.class)).isZero();
    }

    @Test
    @DisplayName("FACILITY the building room list shows floor, room type, rent, amenities, bed count, occupied beds and gender")
    void buildingRoomList() {
        Building b6 = fixture.building("B6");
        bedOccupancy.occupy(b6.bedIds().get(0));
        bedOccupancy.occupy(b6.bedIds().get(1));

        JsonNode rooms = expect(200, admin.get("/api/buildings/" + b6.id() + "/rooms"));

        assertThat(rooms.size()).isEqualTo(1);
        JsonNode room = rooms.get(0);
        assertThat(room.path("code").asString()).isEqualTo("301");
        assertThat(room.path("floorNumber").asInt()).isEqualTo(3);
        assertThat(room.path("floorGenderPreference").asString()).isEqualTo("MALE");
        assertThat(room.path("roomType").path("name").asString()).isEqualTo("8-student room");
        assertThat(room.path("roomType").path("capacity").asInt()).isEqualTo(8);
        assertThat(room.path("roomType").path("monthlyRent").asLong()).isEqualTo(730_000L);
        assertThat(room.path("roomType").path("hasAirConditioning").asBoolean()).isTrue();
        assertThat(room.path("roomType").path("hasWaterHeater").asBoolean()).isTrue();
        assertThat(room.path("roomType").path("bathrooms").asInt()).isEqualTo(1);
        assertThat(room.path("bedCount").asInt()).isEqualTo(8);
        assertThat(room.path("occupiedBedCount").asInt()).isEqualTo(2);
        assertThat(room.path("gender").isNull()).isTrue();
        JsonNode building = expect(200, admin.get("/api/buildings/" + b6.id()));
        assertThat(building.path("floorCount").asInt()).isEqualTo(1);
        assertThat(building.path("roomCount").asInt()).isEqualTo(1);
        assertThat(building.path("bedCount").asInt()).isEqualTo(8);
        assertThat(building.path("occupiedBedCount").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("FACILITY invalid requests return 400 with field errors (blank code, capacity 0, negative rent)")
    void invalidRequests() {
        long b6 = createBuilding("B6");

        ApiResponse blankCode = admin.post("/api/buildings", Map.of("code", " ", "name", "Building"));
        Map<String, Object> badType = roomType("Empty room", 0, -1);
        ApiResponse badRoomType = admin.post("/api/buildings/" + b6 + "/room-types", badType);
        Map<String, Object> missingHeater = roomType("8-student room", 8, 730_000);
        missingHeater.remove("hasWaterHeater");
        ApiResponse incomplete = admin.post("/api/buildings/" + b6 + "/room-types", missingHeater);

        assertThat(blankCode.status()).isEqualTo(400);
        assertThat(blankCode.body().path("errors").findValuesAsString("field")).contains("code");
        assertThat(badRoomType.status()).isEqualTo(400);
        assertThat(badRoomType.body().path("errors").findValuesAsString("field")).contains("capacity", "monthlyRent");
        assertThat(incomplete.status()).isEqualTo(400);
        assertThat(incomplete.body().path("errors").findValuesAsString("field")).contains("hasWaterHeater");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM room_types", Long.class)).isZero();
    }

    private long createBuilding(String code) {
        return expect(201, admin.post("/api/buildings", Map.of("code", code, "name", "Building " + code)))
                .path("id").asLong();
    }

    private JsonNode room(long roomId) {
        return expect(200, admin.get("/api/rooms/" + roomId));
    }

    private static List<String> codes(JsonNode buildings) {
        List<String> codes = new ArrayList<>();
        buildings.forEach(building -> codes.add(building.path("code").asString()));
        return codes;
    }
}
