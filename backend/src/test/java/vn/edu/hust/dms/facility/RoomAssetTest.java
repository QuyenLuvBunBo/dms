package vn.edu.hust.dms.facility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.dms.common.audit.AuditLog;
import vn.edu.hust.dms.common.audit.AuditLogRepository;
import vn.edu.hust.dms.common.audit.AuditSubjectType;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.support.AbstractIntegrationTest;
import vn.edu.hust.dms.support.ApiClient;
import vn.edu.hust.dms.support.FacilityFixture;
import vn.edu.hust.dms.support.FacilityFixture.Building;
import vn.edu.hust.dms.support.TestUsers;
import vn.edu.hust.dms.support.TestUsers.Credentials;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static vn.edu.hust.dms.support.FacilityFixture.expect;

/** Room assets are the one thing a building manager changes in Phase 1; status changes are audited. */
class RoomAssetTest extends AbstractIntegrationTest {

    @Autowired
    private TestUsers testUsers;

    @Autowired
    private AuditLogRepository auditLogs;

    private Credentials adminUser;
    private ApiClient admin;
    private Credentials m6;
    private Building b6;

    @BeforeEach
    void buildB6() {
        adminUser = testUsers.create(Role.ADMIN);
        admin = loggedIn(adminUser);
        FacilityFixture fixture = new FacilityFixture(admin);
        b6 = fixture.building("B6");
        m6 = testUsers.create(Role.BUILDING_MANAGER);
        fixture.link(b6.id(), m6.id());
    }

    @Test
    @DisplayName("FACILITY ADMIN and the room's building manager can add an asset, change its status and remove it")
    void adminAndManagerManageAssets() {
        for (ApiClient client : List.of(admin, loggedIn(m6))) {
            JsonNode desk = expect(201, client.post("/api/rooms/" + b6.roomId() + "/assets",
                    Map.of("name", "Desk", "status", "GOOD")));
            long deskId = desk.path("id").asLong();
            assertThat(desk.path("roomId").asLong()).isEqualTo(b6.roomId());

            JsonNode damaged = expect(200, client.put("/api/room-assets/" + deskId,
                    Map.of("name", "Desk", "status", "DAMAGED")));

            assertThat(damaged.path("status").asString()).isEqualTo("DAMAGED");
            assertThat(assetNames(client)).contains("Desk:DAMAGED");
            expect(204, client.delete("/api/room-assets/" + deskId));
            assertThat(assetNames(client)).containsExactly("Air conditioner:GOOD");
        }
    }

    @Test
    @DisplayName("FACILITY creating an asset and each status change write an audit row (ROOM_ASSET, from, to, user)")
    void assetStatusChangesAreAudited() {
        ApiClient manager = loggedIn(m6);
        long deskId = expect(201, manager.post("/api/rooms/" + b6.roomId() + "/assets",
                Map.of("name", "Desk", "status", "GOOD"))).path("id").asLong();
        expect(200, manager.put("/api/room-assets/" + deskId, Map.of("name", "Desk", "status", "DAMAGED")));
        expect(200, manager.put("/api/room-assets/" + deskId, Map.of("name", "Old desk", "status", "DAMAGED")));
        expect(200, admin.put("/api/room-assets/" + deskId, Map.of("name", "Old desk", "status", "MISSING")));

        List<AuditLog> rows = auditLogs.findBySubjectTypeAndSubjectIdOrderByCreatedAtAscIdAsc(
                AuditSubjectType.ROOM_ASSET, deskId);

        assertThat(rows).as("a rename without a status change is not a transition")
                .extracting(AuditLog::getFromStatus, AuditLog::getToStatus, AuditLog::getUserId)
                .containsExactly(
                        tuple(null, "GOOD", m6.id()),
                        tuple("GOOD", "DAMAGED", m6.id()),
                        tuple("DAMAGED", "MISSING", adminUser.id()));
    }

    private List<String> assetNames(ApiClient client) {
        List<String> names = new ArrayList<>();
        expect(200, client.get("/api/rooms/" + b6.roomId())).path("assets")
                .forEach(asset -> names.add(asset.path("name").asString() + ":" + asset.path("status").asString()));
        return names;
    }
}
