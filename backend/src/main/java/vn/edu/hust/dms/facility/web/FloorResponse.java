package vn.edu.hust.dms.facility.web;

import vn.edu.hust.dms.facility.entity.Floor;
import vn.edu.hust.dms.facility.entity.Gender;

public record FloorResponse(long id, long buildingId, int number, Gender genderPreference, long roomCount) {

    public static FloorResponse from(Floor floor, long roomCount) {
        return new FloorResponse(floor.getId(), floor.getBuilding().getId(), floor.getNumber(),
                floor.getGenderPreference(), roomCount);
    }
}
