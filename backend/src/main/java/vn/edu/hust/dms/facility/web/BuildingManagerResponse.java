package vn.edu.hust.dms.facility.web;

import java.util.List;

/** A BUILDING_MANAGER account and the buildings linked to it, for the assignment screen. */
public record BuildingManagerResponse(long userId, String username, String fullName, List<BuildingRef> buildings) {

    public record BuildingRef(long id, String code) {
    }
}
