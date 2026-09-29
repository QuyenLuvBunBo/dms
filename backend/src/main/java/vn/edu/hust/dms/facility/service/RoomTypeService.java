package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.error.ConflictException;
import vn.edu.hust.dms.facility.entity.Building;
import vn.edu.hust.dms.facility.entity.RoomType;
import vn.edu.hust.dms.facility.repository.BedRepository;
import vn.edu.hust.dms.facility.repository.RoomRepository;
import vn.edu.hust.dms.facility.repository.RoomTypeRepository;
import vn.edu.hust.dms.facility.web.RoomTypeRequest;
import vn.edu.hust.dms.facility.web.RoomTypeResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Room types belong to one building. A room never has more beds than its room type's capacity, so
 * the capacity cannot drop below the bed count of a room that uses the type; raising it adds no beds.
 */
@Service
public class RoomTypeService {

    private final RoomTypeRepository roomTypes;
    private final RoomRepository rooms;
    private final BedRepository beds;
    private final FacilityLookup lookup;
    private final BuildingScope scope;

    public RoomTypeService(RoomTypeRepository roomTypes, RoomRepository rooms, BedRepository beds,
                           FacilityLookup lookup, BuildingScope scope) {
        this.roomTypes = roomTypes;
        this.rooms = rooms;
        this.beds = beds;
        this.lookup = lookup;
        this.scope = scope;
    }

    @Transactional(readOnly = true)
    public List<RoomTypeResponse> list(long buildingId) {
        lookup.building(buildingId);
        Map<Long, Long> roomCounts = roomCountsByType(buildingId);
        return roomTypes.findByBuildingIdOrderByCapacityAscNameAsc(buildingId).stream()
                .map(type -> RoomTypeResponse.from(type, roomCounts.getOrDefault(type.getId(), 0L)))
                .toList();
    }

    @Transactional
    public RoomTypeResponse create(long buildingId, RoomTypeRequest request) {
        Building building = lookup.building(buildingId);
        scope.requireAdmin();
        RoomType.Details details = request.details();
        if (roomTypes.existsByBuildingIdAndName(buildingId, details.name())) {
            throw duplicate(details.name(), building);
        }
        return RoomTypeResponse.from(roomTypes.save(new RoomType(building, details)), 0);
    }

    @Transactional
    public RoomTypeResponse update(long roomTypeId, RoomTypeRequest request) {
        RoomType type = lookup.roomTypeForUpdate(roomTypeId);
        scope.requireAdmin();
        RoomType.Details details = request.details();
        Building building = type.getBuilding();
        if (roomTypes.existsByBuildingIdAndNameAndIdNot(building.getId(), details.name(), roomTypeId)) {
            throw duplicate(details.name(), building);
        }
        long mostBeds = beds.bedCountsPerRoomOfType(roomTypeId).stream().mapToLong(Long::longValue).max().orElse(0);
        if (details.capacity() < mostBeds) {
            throw new ConflictException("The capacity of " + type.getName() + " cannot drop below " + mostBeds
                    + ": a room of this type has " + mostBeds + " beds. Remove beds first");
        }
        type.update(details);
        return RoomTypeResponse.from(type, rooms.countByRoomTypeId(roomTypeId));
    }

    @Transactional
    public void delete(long roomTypeId) {
        RoomType type = lookup.roomType(roomTypeId);
        scope.requireAdmin();
        long used = rooms.countByRoomTypeId(roomTypeId);
        if (used > 0) {
            throw new ConflictException("Room type " + type.getName() + " is used by " + used + " room(s)");
        }
        roomTypes.delete(type);
    }

    private Map<Long, Long> roomCountsByType(long buildingId) {
        return roomTypes.countRoomsPerRoomType(buildingId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private static ConflictException duplicate(String name, Building building) {
        return new ConflictException("A room type named " + name + " already exists in " + building.getCode());
    }
}
