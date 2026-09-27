package vn.edu.hust.dms.demo;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.security.UserPrincipal;
import vn.edu.hust.dms.common.user.UserAccount;
import vn.edu.hust.dms.common.user.UserAccountRepository;
import vn.edu.hust.dms.demo.DemoFacilities.BuildingSpec;
import vn.edu.hust.dms.demo.DemoFacilities.RoomTypeSpec;
import vn.edu.hust.dms.facility.entity.RoomAssetStatus;
import vn.edu.hust.dms.facility.service.BuildingManagerService;
import vn.edu.hust.dms.facility.service.BuildingService;
import vn.edu.hust.dms.facility.service.FloorService;
import vn.edu.hust.dms.facility.service.RoomAssetService;
import vn.edu.hust.dms.facility.service.RoomService;
import vn.edu.hust.dms.facility.service.RoomTypeService;
import vn.edu.hust.dms.facility.web.BuildingRequest;
import vn.edu.hust.dms.facility.web.FloorRequest;
import vn.edu.hust.dms.facility.web.RoomAssetRequest;
import vn.edu.hust.dms.facility.web.RoomRequest;
import vn.edu.hust.dms.facility.web.RoomTypeRequest;

import java.time.Clock;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seeds the demo data set. Kept profile-independent so it can be tested; the demo profile
 * runs it on startup through {@link DemoDataSeeder}. The facilities are created through the same
 * services as the API, acting as the seeded admin, so the seed obeys the same rules (capacity,
 * unique codes, building scope). Later phases add students and a registration round here.
 */
@Service
public class DemoDataService {

    private static final Logger log = LoggerFactory.getLogger(DemoDataService.class);

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final BuildingService buildings;
    private final FloorService floors;
    private final RoomTypeService roomTypes;
    private final RoomService rooms;
    private final RoomAssetService assets;
    private final BuildingManagerService managers;
    private final EntityManager entityManager;

    public DemoDataService(UserAccountRepository users, PasswordEncoder passwordEncoder, Clock clock,
                           BuildingService buildings, FloorService floors, RoomTypeService roomTypes,
                           RoomService rooms, RoomAssetService assets, BuildingManagerService managers,
                           EntityManager entityManager) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.buildings = buildings;
        this.floors = floors;
        this.roomTypes = roomTypes;
        this.rooms = rooms;
        this.assets = assets;
        this.managers = managers;
        this.entityManager = entityManager;
    }

    /** @return true when data was seeded, false when the database already had users */
    @Transactional
    public boolean seedIfEmpty() {
        long existing = users.count();
        if (existing > 0) {
            log.info("Demo data skipped: database already has {} user(s)", existing);
            return false;
        }
        String passwordHash = passwordEncoder.encode(DemoAccount.PASSWORD);
        Map<DemoAccount, UserAccount> accounts = new EnumMap<>(DemoAccount.class);
        for (DemoAccount account : DemoAccount.values()) {
            accounts.put(account, users.save(new UserAccount(account.username(), passwordHash, account.fullName(),
                    account.email(), account.role(), clock.instant())));
        }
        runAs(accounts.get(DemoAccount.ADMIN), () -> seedFacilities(accounts));
        log.info("Seeded demo accounts {} with password '{}' and buildings {}",
                Arrays.stream(DemoAccount.values()).map(DemoAccount::username).collect(Collectors.joining(", ")),
                DemoAccount.PASSWORD,
                DemoFacilities.BUILDINGS.stream().map(BuildingSpec::code).collect(Collectors.joining(", ")));
        return true;
    }

    private void seedFacilities(Map<DemoAccount, UserAccount> accounts) {
        Map<String, Long> buildingIds = new HashMap<>();
        for (BuildingSpec spec : DemoFacilities.BUILDINGS) {
            buildingIds.put(spec.code(), seedBuilding(spec));
            // One building at a time in the persistence context keeps Hibernate's flush checks cheap.
            entityManager.flush();
            entityManager.clear();
        }
        for (DemoAccount account : DemoAccount.values()) {
            account.buildingCode().ifPresent(code ->
                    managers.assign(buildingIds.get(code), accounts.get(account).getId()));
        }
    }

    private long seedBuilding(BuildingSpec spec) {
        long buildingId = buildings.create(new BuildingRequest(spec.code(), "Building " + spec.code())).id();
        Map<Integer, Long> roomTypeIds = new HashMap<>();
        for (RoomTypeSpec type : spec.roomTypes()) {
            roomTypeIds.put(type.capacity(), roomTypes.create(buildingId, new RoomTypeRequest(type.name(),
                    type.capacity(), spec.areaM2(), spec.airConditioning(), spec.waterHeater(), spec.bathrooms(),
                    type.monthlyRent())).id());
        }
        for (int floorNumber = 1; floorNumber <= spec.floors(); floorNumber++) {
            long floorId = floors.create(buildingId,
                    new FloorRequest(floorNumber, DemoFacilities.floorPreference(floorNumber))).id();
            for (int roomOnFloor = 1; roomOnFloor <= DemoFacilities.ROOMS_PER_FLOOR; roomOnFloor++) {
                long roomTypeId = roomTypeIds.get(DemoFacilities.roomTypeOf(spec, roomOnFloor).capacity());
                long roomId = rooms.create(floorId,
                        new RoomRequest(String.valueOf(floorNumber * 100 + roomOnFloor), roomTypeId)).id();
                for (String asset : DemoFacilities.assets(spec)) {
                    RoomAssetStatus status = DemoFacilities.damaged(floorNumber, roomOnFloor, asset)
                            ? RoomAssetStatus.DAMAGED : RoomAssetStatus.GOOD;
                    assets.create(roomId, new RoomAssetRequest(asset, status));
                }
            }
        }
        return buildingId;
    }

    /** Runs the facility seed as the seeded admin, so the services apply their normal checks. */
    private static void runAs(UserAccount account, Runnable action) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        UserPrincipal principal = UserPrincipal.from(account);
        context.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            action.run();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
