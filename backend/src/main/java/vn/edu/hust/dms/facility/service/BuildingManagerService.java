package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.error.InvalidRequestException;
import vn.edu.hust.dms.common.error.NotFoundException;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.common.user.UserAccount;
import vn.edu.hust.dms.common.user.UserAccountRepository;
import vn.edu.hust.dms.facility.entity.Building;
import vn.edu.hust.dms.facility.entity.BuildingManager;
import vn.edu.hust.dms.facility.entity.BuildingManagerId;
import vn.edu.hust.dms.facility.repository.BuildingManagerRepository;
import vn.edu.hust.dms.facility.repository.BuildingRepository;
import vn.edu.hust.dms.facility.web.BuildingManagerResponse;
import vn.edu.hust.dms.facility.web.BuildingManagerResponse.BuildingRef;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The building_managers link (BR-14), managed by ADMIN. A manager may have several buildings. */
@Service
public class BuildingManagerService {

    public static final String USER_NOT_FOUND = "User not found";

    private final BuildingManagerRepository links;
    private final BuildingRepository buildings;
    private final UserAccountRepository users;
    private final FacilityLookup lookup;
    private final BuildingScope scope;

    public BuildingManagerService(BuildingManagerRepository links, BuildingRepository buildings,
                                  UserAccountRepository users, FacilityLookup lookup, BuildingScope scope) {
        this.links = links;
        this.buildings = buildings;
        this.users = users;
        this.lookup = lookup;
        this.scope = scope;
    }

    /** Every BUILDING_MANAGER account with the buildings linked to it. */
    @Transactional(readOnly = true)
    public List<BuildingManagerResponse> list() {
        scope.requireAdmin();
        List<UserAccount> managers = users.findByRoleOrderByFullNameAsc(Role.BUILDING_MANAGER);
        List<BuildingManager> managerLinks = links.findByIdUserIdIn(managers.stream().map(UserAccount::getId).toList());
        Map<Long, Building> buildingsById = buildings
                .findAllById(managerLinks.stream().map(BuildingManager::getBuildingId).distinct().toList()).stream()
                .collect(Collectors.toMap(Building::getId, Function.identity()));
        Map<Long, List<Building>> buildingsByUser = managerLinks.stream().collect(Collectors.groupingBy(
                BuildingManager::getUserId,
                Collectors.mapping(link -> buildingsById.get(link.getBuildingId()), Collectors.toList())));
        return managers.stream()
                .map(user -> new BuildingManagerResponse(user.getId(), user.getUsername(), user.getFullName(),
                        buildingsByUser.getOrDefault(user.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(Building::getCode, FacilityOrder.CODES))
                                .map(building -> new BuildingRef(building.getId(), building.getCode()))
                                .toList()))
                .toList();
    }

    /** Links a BUILDING_MANAGER to a building; linking twice changes nothing. */
    @Transactional
    public void assign(long buildingId, long userId) {
        lookup.building(buildingId);
        scope.requireAdmin();
        UserAccount user = users.findById(userId).orElseThrow(() -> new NotFoundException(USER_NOT_FOUND));
        if (user.getRole() != Role.BUILDING_MANAGER) {
            throw new InvalidRequestException("User " + user.getUsername() + " is not a building manager");
        }
        if (!links.existsByIdUserIdAndIdBuildingId(userId, buildingId)) {
            links.save(new BuildingManager(userId, buildingId));
        }
    }

    /** Removes the link if there is one. The manager loses access on their next request. */
    @Transactional
    public void unassign(long buildingId, long userId) {
        lookup.building(buildingId);
        scope.requireAdmin();
        links.deleteById(new BuildingManagerId(userId, buildingId));
    }
}
