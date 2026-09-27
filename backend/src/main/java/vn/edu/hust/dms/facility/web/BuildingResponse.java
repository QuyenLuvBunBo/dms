package vn.edu.hust.dms.facility.web;

import java.util.List;

/** A building with its size, current occupancy and managers. */
public record BuildingResponse(long id, String code, String name, int floorCount, int roomCount, int bedCount,
                               int occupiedBedCount, List<Manager> managers) {

    public record Manager(long userId, String username, String fullName) {
    }
}
