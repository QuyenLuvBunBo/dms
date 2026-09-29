package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Component;
import vn.edu.hust.dms.common.error.NotFoundException;
import vn.edu.hust.dms.facility.entity.Bed;
import vn.edu.hust.dms.facility.entity.Building;
import vn.edu.hust.dms.facility.entity.Floor;
import vn.edu.hust.dms.facility.entity.Room;
import vn.edu.hust.dms.facility.entity.RoomAsset;
import vn.edu.hust.dms.facility.entity.RoomType;
import vn.edu.hust.dms.facility.repository.BedRepository;
import vn.edu.hust.dms.facility.repository.BuildingRepository;
import vn.edu.hust.dms.facility.repository.FloorRepository;
import vn.edu.hust.dms.facility.repository.RoomAssetRepository;
import vn.edu.hust.dms.facility.repository.RoomRepository;
import vn.edu.hust.dms.facility.repository.RoomTypeRepository;

import java.util.Optional;

/**
 * Loads facility data for the current user. An id that does not exist and an id in a building out
 * of the user's scope (BR-14) both end in the same 404 detail. Every facility service loads through
 * here, so no endpoint can skip the scope check. Runs inside the caller's transaction.
 *
 * <p>The {@code ...ForUpdate} variants lock the row first, before any other read of the transaction,
 * so the reads that follow see what concurrent writers committed before the lock was granted.
 */
@Component
public class FacilityLookup {

    public static final String BUILDING_NOT_FOUND = "Building not found";
    public static final String FLOOR_NOT_FOUND = "Floor not found";
    public static final String ROOM_TYPE_NOT_FOUND = "Room type not found";
    public static final String ROOM_NOT_FOUND = "Room not found";
    public static final String BED_NOT_FOUND = "Bed not found";
    public static final String ASSET_NOT_FOUND = "Room asset not found";

    private final BuildingScope scope;
    private final BuildingRepository buildings;
    private final FloorRepository floors;
    private final RoomTypeRepository roomTypes;
    private final RoomRepository rooms;
    private final BedRepository beds;
    private final RoomAssetRepository assets;

    public FacilityLookup(BuildingScope scope, BuildingRepository buildings, FloorRepository floors,
                          RoomTypeRepository roomTypes, RoomRepository rooms, BedRepository beds,
                          RoomAssetRepository assets) {
        this.scope = scope;
        this.buildings = buildings;
        this.floors = floors;
        this.roomTypes = roomTypes;
        this.rooms = rooms;
        this.beds = beds;
        this.assets = assets;
    }

    public Building building(long id) {
        Building building = found(buildings.findById(id), BUILDING_NOT_FOUND);
        scope.requireVisible(building.getId(), BUILDING_NOT_FOUND);
        return building;
    }

    public Floor floor(long id) {
        Floor floor = found(floors.findById(id), FLOOR_NOT_FOUND);
        scope.requireVisible(floor.getBuilding().getId(), FLOOR_NOT_FOUND);
        return floor;
    }

    public RoomType roomType(long id) {
        return visibleRoomType(found(roomTypes.findById(id), ROOM_TYPE_NOT_FOUND));
    }

    public RoomType roomTypeForUpdate(long id) {
        return visibleRoomType(found(roomTypes.findByIdForUpdate(id), ROOM_TYPE_NOT_FOUND));
    }

    public Room room(long id) {
        return visibleRoom(found(rooms.findById(id), ROOM_NOT_FOUND));
    }

    public Room roomForUpdate(long id) {
        return visibleRoom(found(rooms.findByIdForUpdate(id), ROOM_NOT_FOUND));
    }

    public Bed bed(long id) {
        return visibleBed(found(beds.findById(id), BED_NOT_FOUND));
    }

    public Bed bedForUpdate(long id) {
        return visibleBed(found(beds.findByIdForUpdate(id), BED_NOT_FOUND));
    }

    public RoomAsset asset(long id) {
        RoomAsset asset = found(assets.findById(id), ASSET_NOT_FOUND);
        scope.requireVisible(asset.getRoom().buildingId(), ASSET_NOT_FOUND);
        return asset;
    }

    private RoomType visibleRoomType(RoomType roomType) {
        scope.requireVisible(roomType.getBuilding().getId(), ROOM_TYPE_NOT_FOUND);
        return roomType;
    }

    private Room visibleRoom(Room room) {
        scope.requireVisible(room.buildingId(), ROOM_NOT_FOUND);
        return room;
    }

    private Bed visibleBed(Bed bed) {
        scope.requireVisible(bed.getRoom().buildingId(), BED_NOT_FOUND);
        return bed;
    }

    private static <T> T found(Optional<T> entity, String notFoundDetail) {
        return entity.orElseThrow(() -> new NotFoundException(notFoundDetail));
    }
}
