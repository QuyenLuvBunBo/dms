package vn.edu.hust.dms.demo;

import vn.edu.hust.dms.facility.entity.Gender;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The demo buildings as data.
 * <ul>
 *   <li>Capacities, area, air conditioning, water heater and bathrooms: the Buildings table in
 *       CLAUDE.md (resident interviews).</li>
 *   <li>Rents: official for B6 and B9 (HUST notice for K71); every other rent is a team ESTIMATE.</li>
 *   <li>Floor gender preferences: resident interview.</li>
 *   <li>Floors per building, rooms per floor, the split of a floor's rooms across room types and the
 *       assets per room: ESTIMATE.</li>
 * </ul>
 */
final class DemoFacilities {

    /** ESTIMATE: rooms on every floor, coded floor * 100 + 1 .. floor * 100 + 6. */
    static final int ROOMS_PER_FLOOR = 6;

    record RoomTypeSpec(int capacity, long monthlyRent) {

        String name() {
            return capacity + "-student room";
        }
    }

    record BuildingSpec(String code, int floors, BigDecimal areaM2, boolean airConditioning, boolean waterHeater,
                        int bathrooms, List<RoomTypeSpec> roomTypes) {
    }

    static final List<BuildingSpec> BUILDINGS = List.of(
            // Rent ESTIMATE: same rent per capacity as B6/B9. Floors ESTIMATE.
            building("B3", 4, 38, true, true, 1,
                    rent(10, 550_000)),
            // Rent ESTIMATE: same rent per capacity as B6/B9. Floors ESTIMATE.
            building("B5", 4, 38, true, true, 1,
                    rent(8, 730_000), rent(10, 550_000)),
            // Rent official (HUST notice for K71). Floors ESTIMATE.
            building("B6", 5, 38, true, true, 1,
                    rent(6, 1_050_000), rent(8, 730_000), rent(10, 550_000)),
            // Rent ESTIMATE: same rent per capacity as B6/B9. Floors ESTIMATE.
            building("B8", 5, 30, true, true, 1,
                    rent(6, 1_050_000), rent(8, 730_000)),
            // Rent official (HUST notice for K71). Floors ESTIMATE.
            building("B9", 5, 38, true, true, 1,
                    rent(6, 1_050_000), rent(8, 730_000), rent(10, 550_000)),
            // Rent ESTIMATE (team). No air conditioning: two ceiling fans. Floors ESTIMATE.
            building("B10", 5, 60, false, true, 2,
                    rent(8, 450_000), rent(10, 380_000), rent(12, 320_000)),
            // Rent ESTIMATE (team). No air conditioning: one fan. Floors ESTIMATE.
            building("B13", 5, 30, false, true, 1,
                    rent(6, 650_000), rent(8, 480_000)));

    private DemoFacilities() {
    }

    /** Resident interview: FEMALE on floors 2 and 3, MALE on floors 1 and 4, no preference elsewhere. */
    static Gender floorPreference(int floor) {
        return switch (floor) {
            case 1, 4 -> Gender.MALE;
            case 2, 3 -> Gender.FEMALE;
            default -> null;
        };
    }

    /** ESTIMATE: a floor's rooms split evenly across the building's room types, smallest first. */
    static RoomTypeSpec roomTypeOf(BuildingSpec building, int roomOnFloor) {
        List<RoomTypeSpec> types = building.roomTypes();
        return types.get((roomOnFloor - 1) * types.size() / ROOMS_PER_FLOOR);
    }

    /** ESTIMATE: the assets of every room, following the building's amenities. */
    static List<String> assets(BuildingSpec building) {
        List<String> assets = new ArrayList<>();
        if (building.airConditioning()) {
            assets.add("Air conditioner");
        } else if (building.code().equals("B10")) {
            assets.add("Ceiling fan 1");
            assets.add("Ceiling fan 2");
        } else {
            assets.add("Fan");
        }
        if (building.waterHeater()) {
            assets.add("Water heater");
        }
        assets.add("Wardrobe");
        return assets;
    }

    /** ESTIMATE: a few damaged items, so the asset status is visible in the demo. */
    static boolean damaged(int floor, int roomOnFloor, String asset) {
        return floor == 2 && roomOnFloor == ROOMS_PER_FLOOR && asset.equals("Water heater");
    }

    private static BuildingSpec building(String code, int floors, int areaM2, boolean airConditioning,
                                         boolean waterHeater, int bathrooms, RoomTypeSpec... roomTypes) {
        return new BuildingSpec(code, floors, BigDecimal.valueOf(areaM2), airConditioning, waterHeater, bathrooms,
                List.of(roomTypes));
    }

    private static RoomTypeSpec rent(int capacity, long monthlyRent) {
        return new RoomTypeSpec(capacity, monthlyRent);
    }
}
