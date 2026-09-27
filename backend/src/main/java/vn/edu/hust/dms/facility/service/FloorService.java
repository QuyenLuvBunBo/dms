package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.error.ConflictException;
import vn.edu.hust.dms.facility.entity.Building;
import vn.edu.hust.dms.facility.entity.Floor;
import vn.edu.hust.dms.facility.repository.FloorRepository;
import vn.edu.hust.dms.facility.repository.RoomRepository;
import vn.edu.hust.dms.facility.web.FloorRequest;
import vn.edu.hust.dms.facility.web.FloorResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FloorService {

    private final FloorRepository floors;
    private final RoomRepository rooms;
    private final FacilityLookup lookup;
    private final BuildingScope scope;

    public FloorService(FloorRepository floors, RoomRepository rooms, FacilityLookup lookup, BuildingScope scope) {
        this.floors = floors;
        this.rooms = rooms;
        this.lookup = lookup;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public List<FloorResponse> list(long buildingId) {
        lookup.building(buildingId);
        Map<Long, Long> roomCounts = floors.countRoomsPerFloor(buildingId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return floors.findByBuildingIdOrderByNumberAsc(buildingId).stream()
                .map(floor -> FloorResponse.from(floor, roomCounts.getOrDefault(floor.getId(), 0L)))
                .toList();
    }

    @Transactional
    public FloorResponse create(long buildingId, FloorRequest request) {
        Building building = lookup.building(buildingId);
        scope.requireAdmin();
        if (floors.existsByBuildingIdAndNumber(buildingId, request.number())) {
            throw duplicate(request.number(), building);
        }
        Floor floor = floors.save(new Floor(building, request.number(), request.genderPreference()));
        return FloorResponse.from(floor, 0);
    }

    @Transactional
    public FloorResponse update(long floorId, FloorRequest request) {
        Floor floor = lookup.floor(floorId);
        scope.requireAdmin();
        Building building = floor.getBuilding();
        if (floors.existsByBuildingIdAndNumberAndIdNot(building.getId(), request.number(), floorId)) {
            throw duplicate(request.number(), building);
        }
        floor.update(request.number(), request.genderPreference());
        return FloorResponse.from(floor, rooms.countByFloorId(floorId));
    }

    @Transactional
    public void delete(long floorId) {
        Floor floor = lookup.floor(floorId);
        scope.requireAdmin();
        if (rooms.existsByFloorId(floorId)) {
            throw new ConflictException("Floor " + floor.getNumber() + " of " + floor.getBuilding().getCode()
                    + " still has rooms; delete them first");
        }
        floors.delete(floor);
    }

    private static ConflictException duplicate(int number, Building building) {
        return new ConflictException("Floor " + number + " already exists in " + building.getCode());
    }
}
