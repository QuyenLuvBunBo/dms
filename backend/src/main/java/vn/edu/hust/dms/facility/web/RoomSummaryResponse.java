package vn.edu.hust.dms.facility.web;

import vn.edu.hust.dms.facility.entity.Gender;

/** One row of a building's room list. {@code gender} is null while the room is empty (BR-03). */
public record RoomSummaryResponse(long id, String code, long floorId, int floorNumber, Gender floorGenderPreference,
                                  RoomTypeResponse roomType, int bedCount, int occupiedBedCount, Gender gender) {
}
