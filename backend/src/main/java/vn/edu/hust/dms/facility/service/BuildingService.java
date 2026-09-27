package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.error.ConflictException;
import vn.edu.hust.dms.common.user.UserAccount;
import vn.edu.hust.dms.common.user.UserAccountRepository;
import vn.edu.hust.dms.facility.entity.BuildingManager;
import vn.edu.hust.dms.facility.entity.Building;
import vn.edu.hust.dms.facility.repository.BedRepository;
import vn.edu.hust.dms.facility.repository.BuildingManagerRepository;
import vn.edu.hust.dms.facility.repository.BuildingRepository;
import vn.edu.hust.dms.facility.repository.FloorRepository;
import vn.edu.hust.dms.facility.repository.RoomTypeRepository;
import vn.edu.hust.dms.facility.service.BuildingScope.Visibility;
import vn.edu.hust.dms.facility.web.BuildingRequest;
import vn.edu.hust.dms.facility.web.BuildingResponse;

import java.util.Comparator;
import java.util.List;

@Service
public class BuildingService {

    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final RoomTypeRepository roomTypes;
    private final BedRepository beds;
    private final BuildingManagerRepository managers;
    private final UserAccountRepository users;
    private final FacilityLookup lookup;
    private final BuildingScope scope;
    private final OccupancyService occupancy;

    public BuildingService(BuildingRepository buildings, FloorRepository floors, RoomTypeRepository roomTypes,
                           BedRepository beds, BuildingManagerRepository managers, UserAccountRepository users,
                           FacilityLookup lookup, BuildingScope scope, OccupancyService occupancy) {
        this.buildings = buildings;
        this.floors = floors;
        this.roomTypes = roomTypes;
        this.beds = beds;
        this.managers = managers;
        this.users = users;
        this.lookup = lookup;
        this.scope = scope;
        this.occupancy = occupancy;
    }

    /** Every building for ADMIN, the linked ones for a building manager (BR-14). */
    @Transactional(readOnly = true)
    public List<BuildingResponse> list() {
        Visibility visible = scope.visible();
        List<Building> result = visible.all() ? buildings.findAll() : buildings.findByIdIn(visible.buildingIds());
        return result.stream()
                .sorted(Comparator.comparing(Building::getCode, FacilityOrder.CODES))
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuildingResponse get(long id) {
        return response(lookup.building(id));
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        scope.requireAdmin();
        String code = request.code().trim();
        if (buildings.existsByCode(code)) {
            throw new ConflictException("A building with code " + code + " already exists");
        }
        return response(buildings.save(new Building(code, request.name().trim())));
    }

    @Transactional
    public BuildingResponse update(long id, BuildingRequest request) {
        Building building = lookup.building(id);
        scope.requireAdmin();
        String code = request.code().trim();
        if (buildings.existsByCodeAndIdNot(code, id)) {
            throw new ConflictException("A building with code " + code + " already exists");
        }
        building.update(code, request.name().trim());
        return response(building);
    }

    /** Buildings are deleted last: floors and room types go first. Manager links go with the building. */
    @Transactional
    public void delete(long id) {
        Building building = lookup.building(id);
        scope.requireAdmin();
        if (floors.existsByBuildingId(id) || roomTypes.existsByBuildingId(id)) {
            throw new ConflictException("Building " + building.getCode()
                    + " still has floors or room types; delete them first");
        }
        managers.deleteByBuilding(id);
        buildings.delete(building);
    }

    private BuildingResponse response(Building building) {
        long id = building.getId();
        List<Object[]> bedsAndRooms = beds.findBedAndRoomIdsByBuilding(id);
        List<Long> bedIds = bedsAndRooms.stream().map(row -> (Long) row[0]).toList();
        int roomCount = floors.countRoomsPerFloor(id).stream().mapToInt(row -> ((Long) row[1]).intValue()).sum();
        List<Long> managerIds = managers.findByIdBuildingId(id).stream().map(BuildingManager::getUserId).toList();
        List<BuildingResponse.Manager> managerViews = users.findAllById(managerIds).stream()
                .sorted(Comparator.comparing(UserAccount::getFullName))
                .map(user -> new BuildingResponse.Manager(user.getId(), user.getUsername(), user.getFullName()))
                .toList();
        return new BuildingResponse(id, building.getCode(), building.getName(),
                (int) floors.countByBuildingId(id), roomCount, bedIds.size(),
                occupancy.occupiedBedIds(bedIds).size(), managerViews);
    }
}
