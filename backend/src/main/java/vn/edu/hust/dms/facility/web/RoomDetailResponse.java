package vn.edu.hust.dms.facility.web;

import vn.edu.hust.dms.facility.entity.Gender;

import java.util.List;

public record RoomDetailResponse(long id, String code, long buildingId, String buildingCode, long floorId,
                                 int floorNumber, Gender floorGenderPreference, RoomTypeResponse roomType,
                                 Gender gender, List<BedResponse> beds, List<RoomAssetResponse> assets) {
}
