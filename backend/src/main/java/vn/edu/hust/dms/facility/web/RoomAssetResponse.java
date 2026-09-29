package vn.edu.hust.dms.facility.web;

import vn.edu.hust.dms.facility.entity.RoomAsset;
import vn.edu.hust.dms.facility.entity.RoomAssetStatus;

public record RoomAssetResponse(long id, long roomId, String name, RoomAssetStatus status) {

    public static RoomAssetResponse from(RoomAsset asset) {
        return new RoomAssetResponse(asset.getId(), asset.getRoom().getId(), asset.getName(), asset.getStatus());
    }
}
