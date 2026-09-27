package vn.edu.hust.dms.support;

import org.springframework.http.HttpMethod;
import vn.edu.hust.dms.support.ApiClient.ApiResponse;
import vn.edu.hust.dms.support.FacilityFixture.Building;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

/**
 * The catalogue of every building-scoped facility endpoint. The BR-14 and access tests are
 * parameterized over it, so an endpoint added to the API must be added here to be covered.
 * Endpoints without a building (GET/POST /api/buildings, GET /api/building-managers) are tested
 * on their own.
 */
public final class FacilityEndpoints {

    /** Who may call the endpoint inside their own building. */
    public enum Access { MANAGER, ADMIN_ONLY }

    /**
     * One endpoint: its path and a valid body are built from the target building's fixture ids;
     * {@code userId} is the user a manager-link endpoint refers to.
     */
    public record Endpoint(String name, HttpMethod method, BiFunction<Building, Long, String> path,
                           Function<Building, Object> body, Access access) {

        public ApiResponse call(ApiClient api, Building target, long userId) {
            return api.send(method, path.apply(target, userId), body.apply(target));
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final List<Endpoint> ALL = List.of(
            endpoint("GET /api/buildings/{b}", GET, (b, u) -> "/api/buildings/" + b.id(), none(), Access.MANAGER),
            endpoint("PUT /api/buildings/{b}", PUT, (b, u) -> "/api/buildings/" + b.id(),
                    b -> Map.of("code", b.code(), "name", "Renamed " + b.code()), Access.ADMIN_ONLY),
            endpoint("DELETE /api/buildings/{b}", DELETE, (b, u) -> "/api/buildings/" + b.id(), none(), Access.ADMIN_ONLY),

            endpoint("GET /api/buildings/{b}/floors", GET, (b, u) -> "/api/buildings/" + b.id() + "/floors", none(),
                    Access.MANAGER),
            endpoint("POST /api/buildings/{b}/floors", POST, (b, u) -> "/api/buildings/" + b.id() + "/floors",
                    b -> Map.of("number", 7), Access.ADMIN_ONLY),
            endpoint("PUT /api/floors/{f}", PUT, (b, u) -> "/api/floors/" + b.floorId(),
                    b -> Map.of("number", 3, "genderPreference", "FEMALE"), Access.ADMIN_ONLY),
            endpoint("DELETE /api/floors/{f}", DELETE, (b, u) -> "/api/floors/" + b.floorId(), none(), Access.ADMIN_ONLY),

            endpoint("GET /api/buildings/{b}/room-types", GET, (b, u) -> "/api/buildings/" + b.id() + "/room-types",
                    none(), Access.MANAGER),
            endpoint("POST /api/buildings/{b}/room-types", POST, (b, u) -> "/api/buildings/" + b.id() + "/room-types",
                    b -> FacilityFixture.roomType("6-student room", 6, 1_050_000), Access.ADMIN_ONLY),
            endpoint("PUT /api/room-types/{t}", PUT, (b, u) -> "/api/room-types/" + b.roomTypeId(),
                    b -> FacilityFixture.roomType("8-student room", 8, 750_000), Access.ADMIN_ONLY),
            endpoint("DELETE /api/room-types/{t}", DELETE, (b, u) -> "/api/room-types/" + b.roomTypeId(), none(),
                    Access.ADMIN_ONLY),

            endpoint("GET /api/buildings/{b}/rooms", GET, (b, u) -> "/api/buildings/" + b.id() + "/rooms", none(),
                    Access.MANAGER),
            endpoint("POST /api/floors/{f}/rooms", POST, (b, u) -> "/api/floors/" + b.floorId() + "/rooms",
                    b -> Map.of("code", "302", "roomTypeId", b.roomTypeId()), Access.ADMIN_ONLY),
            endpoint("GET /api/rooms/{r}", GET, (b, u) -> "/api/rooms/" + b.roomId(), none(), Access.MANAGER),
            endpoint("PUT /api/rooms/{r}", PUT, (b, u) -> "/api/rooms/" + b.roomId(),
                    b -> Map.of("code", "301A", "roomTypeId", b.roomTypeId()), Access.ADMIN_ONLY),
            endpoint("DELETE /api/rooms/{r}", DELETE, (b, u) -> "/api/rooms/" + b.roomId(), none(), Access.ADMIN_ONLY),

            endpoint("POST /api/rooms/{r}/beds", POST, (b, u) -> "/api/rooms/" + b.roomId() + "/beds",
                    b -> Map.of(), Access.ADMIN_ONLY),
            endpoint("PUT /api/beds/{bed}", PUT, (b, u) -> "/api/beds/" + b.bedId(), b -> Map.of("code", "1A"),
                    Access.ADMIN_ONLY),
            endpoint("DELETE /api/beds/{bed}", DELETE, (b, u) -> "/api/beds/" + b.bedId(), none(), Access.ADMIN_ONLY),

            endpoint("POST /api/rooms/{r}/assets", POST, (b, u) -> "/api/rooms/" + b.roomId() + "/assets",
                    b -> Map.of("name", "Desk", "status", "GOOD"), Access.MANAGER),
            endpoint("PUT /api/room-assets/{a}", PUT, (b, u) -> "/api/room-assets/" + b.assetId(),
                    b -> Map.of("name", "Air conditioner", "status", "DAMAGED"), Access.MANAGER),
            endpoint("DELETE /api/room-assets/{a}", DELETE, (b, u) -> "/api/room-assets/" + b.assetId(), none(),
                    Access.MANAGER),

            endpoint("PUT /api/buildings/{b}/managers/{userId}", PUT,
                    (b, u) -> "/api/buildings/" + b.id() + "/managers/" + u, none(), Access.ADMIN_ONLY),
            endpoint("DELETE /api/buildings/{b}/managers/{userId}", DELETE,
                    (b, u) -> "/api/buildings/" + b.id() + "/managers/" + u, none(), Access.ADMIN_ONLY));

    private FacilityEndpoints() {
    }

    public static Stream<Endpoint> all() {
        return ALL.stream();
    }

    /** The endpoints a building manager may call in their own building (reads and room assets). */
    public static Stream<Endpoint> managerAllowed() {
        return all().filter(endpoint -> endpoint.access() == Access.MANAGER);
    }

    public static Stream<Endpoint> adminOnly() {
        return all().filter(endpoint -> endpoint.access() == Access.ADMIN_ONLY);
    }

    private static Endpoint endpoint(String name, HttpMethod method, BiFunction<Building, Long, String> path,
                                     Function<Building, Object> body, Access access) {
        return new Endpoint(name, method, path, body, access);
    }

    private static Function<Building, Object> none() {
        return b -> null;
    }
}
