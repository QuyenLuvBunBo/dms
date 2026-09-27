package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.error.ConflictException;
import vn.edu.hust.dms.common.error.InvalidRequestException;
import vn.edu.hust.dms.facility.entity.Bed;
import vn.edu.hust.dms.facility.entity.Building;
import vn.edu.hust.dms.facility.entity.Floor;
import vn.edu.hust.dms.facility.entity.Room;
import vn.edu.hust.dms.facility.entity.RoomType;
import vn.edu.hust.dms.facility.repository.BedRepository;
import vn.edu.hust.dms.facility.repository.RoomAssetRepository;
import vn.edu.hust.dms.facility.repository.RoomRepository;
import vn.edu.hust.dms.facility.repository.RoomTypeRepository;
import vn.edu.hust.dms.facility.web.AddBedRequest;
import vn.edu.hust.dms.facility.web.BedRequest;
import vn.edu.hust.dms.facility.web.BedResponse;
import vn.edu.hust.dms.facility.web.RoomAssetResponse;
import vn.edu.hust.dms.facility.web.RoomDetailResponse;
import vn.edu.hust.dms.facility.web.RoomRequest;
import vn.edu.hust.dms.facility.web.RoomSummaryResponse;
import vn.edu.hust.dms.facility.web.RoomTypeResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Rooms and their beds. A new room gets exactly as many beds as its room type's capacity, coded
 * 1..N; afterwards ADMIN may remove a free bed or add one, but a room never has more beds than the
 * capacity. A room or bed with an occupant is never deleted ({@link OccupancyService}).
 */
@Service
public class RoomService {

    private final RoomRepository rooms;
    private final RoomTypeRepository roomTypes;
    private final BedRepository beds;
    private final RoomAssetRepository assets;
    private final FacilityLookup lookup;
    private final BuildingScope scope;
    private final OccupancyService occupancy;

    public RoomService(RoomRepository rooms, RoomTypeRepository roomTypes, BedRepository beds,
                       RoomAssetRepository assets, FacilityLookup lookup, BuildingScope scope,
                       OccupancyService occupancy) {
        this.rooms = rooms;
        this.roomTypes = roomTypes;
        this.beds = beds;
        this.assets = assets;
        this.lookup = lookup;
        this.scope = scope;
        this.occupancy = occupancy;
    }

    @Transactional(readOnly = true)
    public List<RoomSummaryResponse> list(long buildingId) {
        lookup.building(buildingId);
        Map<Long, List<Long>> bedIdsByRoom = new HashMap<>();
        for (Object[] row : beds.findBedAndRoomIdsByBuilding(buildingId)) {
            bedIdsByRoom.computeIfAbsent((Long) row[1], roomId -> new ArrayList<>()).add((Long) row[0]);
        }
        Set<Long> occupied = occupancy.occupiedBedIds(
                bedIdsByRoom.values().stream().flatMap(List::stream).toList());
        Map<Long, Long> roomsPerType = roomTypes.countRoomsPerRoomType(buildingId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return rooms.findByBuildingWithFloorAndType(buildingId).stream()
                .sorted(Comparator.comparingInt((Room room) -> room.getFloor().getNumber())
                        .thenComparing(Room::getCode, FacilityOrder.CODES))
                .map(room -> {
                    List<Long> bedIds = bedIdsByRoom.getOrDefault(room.getId(), List.of());
                    Floor floor = room.getFloor();
                    RoomType type = room.getRoomType();
                    return new RoomSummaryResponse(room.getId(), room.getCode(), floor.getId(), floor.getNumber(),
                            floor.getGenderPreference(),
                            RoomTypeResponse.from(type, roomsPerType.getOrDefault(type.getId(), 0L)),
                            bedIds.size(), (int) bedIds.stream().filter(occupied::contains).count(),
                            room.getGender());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomDetailResponse get(long roomId) {
        return detail(lookup.room(roomId));
    }

    @Transactional
    public RoomDetailResponse create(long floorId, RoomRequest request) {
        Floor floor = lookup.floor(floorId);
        scope.requireAdmin();
        Building building = floor.getBuilding();
        RoomType type = roomTypeOf(building, request.roomTypeId());
        String code = request.code().trim();
        if (rooms.existsByFloorBuildingIdAndCode(building.getId(), code)) {
            throw duplicateRoom(code, building);
        }
        Room room = rooms.save(new Room(floor, type, code));
        for (int number = 1; number <= type.getCapacity(); number++) {
            beds.save(new Bed(room, String.valueOf(number)));
        }
        return detail(room);
    }

    /** Changes the code or the room type; the beds stay, so the new type must fit them. */
    @Transactional
    public RoomDetailResponse update(long roomId, RoomRequest request) {
        Room room = lookup.roomForUpdate(roomId);
        scope.requireAdmin();
        Building building = room.getFloor().getBuilding();
        RoomType type = roomTypeOf(building, request.roomTypeId());
        String code = request.code().trim();
        if (rooms.existsCodeInBuildingExcept(building.getId(), code, roomId)) {
            throw duplicateRoom(code, building);
        }
        long bedCount = beds.countByRoomId(roomId);
        if (bedCount > type.getCapacity()) {
            throw new ConflictException("Room " + room.getCode() + " has " + bedCount + " beds but room type "
                    + type.getName() + " allows " + type.getCapacity() + ". Remove beds first");
        }
        room.update(code, type);
        return detail(room);
    }

    /** Deletes the room with its beds and assets, unless one of its beds has an occupant. */
    @Transactional
    public void delete(long roomId) {
        Room room = lookup.roomForUpdate(roomId);
        scope.requireAdmin();
        List<Bed> roomBeds = beds.findByRoomIdForUpdate(roomId);
        Set<Long> occupied = occupancy.occupiedBedIds(roomBeds.stream().map(Bed::getId).toList());
        if (!occupied.isEmpty()) {
            throw new ConflictException("Room " + room.getCode() + " has " + occupied.size()
                    + " occupied bed(s) and cannot be deleted");
        }
        assets.deleteAll(assets.findByRoomIdOrderByNameAscIdAsc(roomId));
        beds.deleteAll(roomBeds);
        rooms.delete(room);
    }

    /** Adds one bed, up to the room type's capacity; without a code it gets the lowest free number. */
    @Transactional
    public BedResponse addBed(long roomId, AddBedRequest request) {
        Room room = lookup.roomForUpdate(roomId);
        scope.requireAdmin();
        RoomType type = roomTypes.findByIdForUpdate(room.getRoomType().getId()).orElseThrow();
        List<Bed> existing = beds.findByRoomId(roomId);
        if (existing.size() >= type.getCapacity()) {
            throw new ConflictException("Room " + room.getCode() + " already has " + existing.size()
                    + " beds, the capacity of its room type");
        }
        String requested = request == null ? null : request.code();
        String code = requested == null || requested.isBlank() ? lowestFreeNumber(existing) : requested.trim();
        if (beds.existsByRoomIdAndCode(roomId, code)) {
            throw duplicateBed(code, room);
        }
        Bed bed = beds.save(new Bed(room, code));
        return new BedResponse(bed.getId(), bed.getCode(), false);
    }

    @Transactional
    public BedResponse relabelBed(long bedId, BedRequest request) {
        Bed bed = lookup.bed(bedId);
        scope.requireAdmin();
        Room room = bed.getRoom();
        String code = request.code().trim();
        if (beds.existsByRoomIdAndCodeAndIdNot(room.getId(), code, bedId)) {
            throw duplicateBed(code, room);
        }
        bed.relabel(code);
        return new BedResponse(bedId, code, occupancy.isOccupied(bedId));
    }

    /** Removes one free bed; an occupied bed stays. */
    @Transactional
    public void removeBed(long bedId) {
        Bed bed = lookup.bedForUpdate(bedId);
        scope.requireAdmin();
        if (occupancy.isOccupied(bedId)) {
            throw new ConflictException("Bed " + bed.getCode() + " of room " + bed.getRoom().getCode()
                    + " is occupied and cannot be removed");
        }
        beds.delete(bed);
    }

    private RoomType roomTypeOf(Building building, long roomTypeId) {
        RoomType type = lookup.roomType(roomTypeId);
        if (!type.getBuilding().getId().equals(building.getId())) {
            throw new InvalidRequestException("Room type " + type.getName() + " belongs to building "
                    + type.getBuilding().getCode() + ", not " + building.getCode());
        }
        return type;
    }

    private RoomDetailResponse detail(Room room) {
        Floor floor = room.getFloor();
        Building building = floor.getBuilding();
        RoomType type = room.getRoomType();
        List<Bed> roomBeds = beds.findByRoomId(room.getId()).stream()
                .sorted(Comparator.comparing(Bed::getCode, FacilityOrder.CODES))
                .toList();
        Set<Long> occupied = occupancy.occupiedBedIds(roomBeds.stream().map(Bed::getId).toList());
        List<BedResponse> bedViews = roomBeds.stream()
                .map(bed -> new BedResponse(bed.getId(), bed.getCode(), occupied.contains(bed.getId())))
                .toList();
        List<RoomAssetResponse> assetViews = assets.findByRoomIdOrderByNameAscIdAsc(room.getId()).stream()
                .map(RoomAssetResponse::from)
                .toList();
        return new RoomDetailResponse(room.getId(), room.getCode(), building.getId(), building.getCode(),
                floor.getId(), floor.getNumber(), floor.getGenderPreference(),
                RoomTypeResponse.from(type, rooms.countByRoomTypeId(type.getId())), room.getGender(),
                bedViews, assetViews);
    }

    private static String lowestFreeNumber(List<Bed> existing) {
        Set<String> taken = existing.stream()
                .map(bed -> bed.getCode().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        int number = 1;
        while (taken.contains(String.valueOf(number))) {
            number++;
        }
        return String.valueOf(number);
    }

    private static ConflictException duplicateRoom(String code, Building building) {
        return new ConflictException("A room with code " + code + " already exists in " + building.getCode());
    }

    private static ConflictException duplicateBed(String code, Room room) {
        return new ConflictException("Bed " + code + " already exists in room " + room.getCode());
    }
}
