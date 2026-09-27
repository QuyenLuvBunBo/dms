package vn.edu.hust.dms.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.common.user.UserAccount;
import vn.edu.hust.dms.common.user.UserAccountRepository;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static vn.edu.hust.dms.support.FacilityFixture.expect;

class DemoDataServiceTest extends AbstractIntegrationTest {

    @Autowired
    private DemoDataService demoData;

    @Autowired
    private UserAccountRepository users;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("DEMO seedIfEmpty() on an empty database creates admin, student, technician, accountant and one BUILDING_MANAGER per building")
    void seedsTheDemoAccounts() {
        assertThat(demoData.seedIfEmpty()).isTrue();

        assertThat(users.count()).isEqualTo(DemoAccount.values().length);
        for (Role role : List.of(Role.ADMIN, Role.STUDENT, Role.TECHNICIAN, Role.ACCOUNTANT)) {
            assertThat(users.countByRole(role)).as(role.name()).isEqualTo(1);
        }
        assertThat(users.countByRole(Role.BUILDING_MANAGER)).isEqualTo(7);
        assertThat(Arrays.stream(DemoAccount.values()).flatMap(account -> account.buildingCode().stream()))
                .containsExactlyInAnyOrder("B3", "B5", "B6", "B8", "B9", "B10", "B13");
        assertThat(users.findAll()).allSatisfy(account -> {
            assertThat(account.isEnabled()).isTrue();
            assertThat(account.getPasswordHash()).startsWith("{bcrypt}");
            assertThat(account.getCreatedAt()).isEqualTo(clock.instant());
        });
        assertThat(users.findByUsername("student")).get()
                .extracting(UserAccount::getFullName, UserAccount::getRole)
                .containsExactly("Nguyễn Văn An", Role.STUDENT);
    }

    @Test
    @DisplayName("DEMO seedIfEmpty() is idempotent: a second run creates nothing")
    void secondRunIsNoOp() {
        assertThat(demoData.seedIfEmpty()).isTrue();

        assertThat(demoData.seedIfEmpty()).isFalse();

        assertThat(users.count()).isEqualTo(DemoAccount.values().length);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM buildings", Long.class)).isEqualTo(7L);
    }

    @Test
    @DisplayName("DEMO every seeded account can log in through POST /api/auth/login")
    void everyDemoAccountCanLogIn() {
        demoData.seedIfEmpty();

        for (DemoAccount account : DemoAccount.values()) {
            ApiResponse response = api().loginAs(account.username(), DemoAccount.PASSWORD);

            assertThat(response.status()).as(account.username()).isEqualTo(200);
            assertThat(response.body().path("role").asString()).isEqualTo(account.role().name());
            assertThat(response.body().path("fullName").asString()).isEqualTo(account.fullName());
        }
    }

    @Test
    @DisplayName("DEMO seeds B3, B5, B6, B8, B9, B10 and B13 with floors, room types and rooms whose bed count equals their room type's capacity")
    void seedsTheBuildings() {
        demoData.seedIfEmpty();

        assertThat(jdbc.queryForList("SELECT code FROM buildings", String.class))
                .containsExactlyInAnyOrder("B3", "B5", "B6", "B8", "B9", "B10", "B13");
        assertThat(count("SELECT COUNT(*) FROM buildings b"
                + " WHERE NOT EXISTS (SELECT 1 FROM floors f WHERE f.building_id = b.id)"
                + " OR NOT EXISTS (SELECT 1 FROM room_types t WHERE t.building_id = b.id)"
                + " OR NOT EXISTS (SELECT 1 FROM rooms r JOIN floors f ON f.id = r.floor_id WHERE f.building_id = b.id)"))
                .as("buildings without floors, room types or rooms").isZero();
        assertThat(count("SELECT COUNT(*) FROM rooms r JOIN room_types t ON t.id = r.room_type_id"
                + " WHERE (SELECT COUNT(*) FROM beds b WHERE b.room_id = r.id) <> t.capacity"))
                .as("rooms whose bed count differs from the capacity").isZero();
        assertThat(count("SELECT COUNT(*) FROM rooms r JOIN floors f ON f.id = r.floor_id"
                + " JOIN room_types t ON t.id = r.room_type_id WHERE t.building_id <> f.building_id"))
                .as("rooms using a room type of another building").isZero();
        assertThat(count("SELECT COUNT(*) FROM rooms WHERE gender IS NOT NULL")).isZero();
    }

    @Test
    @DisplayName("DEMO each building's room types match the CLAUDE.md Buildings table: capacities, area, air conditioning, water heater and bathrooms")
    void roomTypesFollowTheBuildingsTable() {
        demoData.seedIfEmpty();

        List<String> expected = new ArrayList<>();
        expected.addAll(buildingsTableRow("B3", 38, true, true, 1, 10));
        expected.addAll(buildingsTableRow("B5", 38, true, true, 1, 8, 10));
        expected.addAll(buildingsTableRow("B6", 38, true, true, 1, 6, 8, 10));
        expected.addAll(buildingsTableRow("B8", 30, true, true, 1, 6, 8));
        expected.addAll(buildingsTableRow("B9", 38, true, true, 1, 6, 8, 10));
        expected.addAll(buildingsTableRow("B10", 60, false, true, 2, 8, 10, 12));
        expected.addAll(buildingsTableRow("B13", 30, false, true, 1, 6, 8));
        List<String> seeded = jdbc.query("SELECT b.code, t.capacity, t.area_m2, t.has_air_conditioning,"
                        + " t.has_water_heater, t.bathrooms FROM room_types t JOIN buildings b ON b.id = t.building_id",
                (rs, row) -> describe(rs.getString(1), rs.getInt(2), rs.getBigDecimal(3), rs.getBoolean(4),
                        rs.getBoolean(5), rs.getInt(6)));

        assertThat(seeded).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    @DisplayName("DEMO B6 and B9 room types carry the official rents: 6-student 1,050,000, 8-student 730,000, 10-student 550,000")
    void officialRents() {
        demoData.seedIfEmpty();

        for (String building : List.of("B6", "B9")) {
            assertThat(rents(building)).as(building)
                    .containsExactly(entry(6, 1_050_000L), entry(8, 730_000L), entry(10, 550_000L));
        }
    }

    @Test
    @DisplayName("DEMO rents outside B6 and B9 are the team's estimates: air-conditioned buildings as B6/B9 per capacity, B10 450,000 / 380,000 / 320,000, B13 650,000 / 480,000")
    void estimatedRents() {
        demoData.seedIfEmpty();

        assertThat(rents("B3")).containsExactly(entry(10, 550_000L));
        assertThat(rents("B5")).containsExactly(entry(8, 730_000L), entry(10, 550_000L));
        assertThat(rents("B8")).containsExactly(entry(6, 1_050_000L), entry(8, 730_000L));
        assertThat(rents("B10")).containsExactly(entry(8, 450_000L), entry(10, 380_000L), entry(12, 320_000L));
        assertThat(rents("B13")).containsExactly(entry(6, 650_000L), entry(8, 480_000L));
    }

    @Test
    @DisplayName("DEMO in every building floors 2 and 3 prefer FEMALE, floors 1 and 4 prefer MALE, and other floors have no preference")
    void floorGenderPreferencesFollowTheResidentInterview() {
        demoData.seedIfEmpty();

        List<Map<String, Object>> floors = jdbc.queryForList("SELECT number, gender_preference FROM floors");

        assertThat(floors).isNotEmpty().allSatisfy(floor -> {
            int number = ((Number) floor.get("number")).intValue();
            Object expected = switch (number) {
                case 1, 4 -> "MALE";
                case 2, 3 -> "FEMALE";
                default -> null;
            };
            assertThat(floor.get("gender_preference")).as("floor " + number).isEqualTo(expected);
        });
        assertThat(floors).as("floors above 4 exist, so the no-preference case is covered")
                .anyMatch(floor -> ((Number) floor.get("number")).intValue() > 4);
    }

    @Test
    @DisplayName("DEMO each demo manager is linked only to their own building, and manager.b6 gets 404 for a B9 room")
    void managersAreLinkedToTheirOwnBuilding() {
        demoData.seedIfEmpty();

        for (DemoAccount account : DemoAccount.values()) {
            List<String> linked = jdbc.queryForList("SELECT b.code FROM building_managers m"
                            + " JOIN buildings b ON b.id = m.building_id JOIN users u ON u.id = m.user_id"
                            + " WHERE u.username = ?", String.class, account.username());
            assertThat(linked).as(account.username())
                    .containsExactlyElementsOf(account.buildingCode().stream().toList());
        }
        ApiClient manager = api();
        expect(200, manager.loginAs("manager.b6", DemoAccount.PASSWORD));
        assertThat(manager.get("/api/rooms/" + firstRoomOf("B9")).status()).isEqualTo(404);
        assertThat(manager.get("/api/rooms/" + firstRoomOf("B6")).status()).isEqualTo(200);
    }

    private long count(String sql) {
        Long count = jdbc.queryForObject(sql, Long.class);
        return count == null ? 0 : count;
    }

    private Map<Integer, Long> rents(String building) {
        Map<Integer, Long> rents = new LinkedHashMap<>();
        jdbc.query("SELECT t.capacity, t.monthly_rent FROM room_types t JOIN buildings b ON b.id = t.building_id"
                        + " WHERE b.code = ? ORDER BY t.capacity",
                rs -> {
                    rents.put(rs.getInt(1), rs.getLong(2));
                }, building);
        return rents;
    }

    private long firstRoomOf(String building) {
        Long id = jdbc.queryForObject("SELECT MIN(r.id) FROM rooms r JOIN floors f ON f.id = r.floor_id"
                + " JOIN buildings b ON b.id = f.building_id WHERE b.code = ?", Long.class, building);
        assertThat(id).as("a room in " + building).isNotNull();
        return id;
    }

    private static List<String> buildingsTableRow(String building, int areaM2, boolean airConditioning,
                                                  boolean waterHeater, int bathrooms, int... capacities) {
        return Arrays.stream(capacities)
                .mapToObj(capacity -> describe(building, capacity, BigDecimal.valueOf(areaM2), airConditioning,
                        waterHeater, bathrooms))
                .toList();
    }

    private static String describe(String building, int capacity, BigDecimal areaM2, boolean airConditioning,
                                   boolean waterHeater, int bathrooms) {
        return building + " capacity " + capacity + ", " + areaM2.stripTrailingZeros().toPlainString()
                + " m2, air con " + airConditioning + ", water heater " + waterHeater + ", bathrooms " + bathrooms;
    }
}
