package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.dms.common.audit.AuditService;
import vn.edu.hust.dms.common.audit.AuditSubjectType;
import vn.edu.hust.dms.facility.entity.Room;
import vn.edu.hust.dms.facility.entity.RoomAsset;
import vn.edu.hust.dms.facility.entity.RoomAssetStatus;
import vn.edu.hust.dms.facility.repository.RoomAssetRepository;
import vn.edu.hust.dms.facility.web.RoomAssetRequest;
import vn.edu.hust.dms.facility.web.RoomAssetResponse;

/**
 * Room inventory, managed by ADMIN and by the building manager of the room's building (the scope
 * check in {@link FacilityLookup} is the only restriction). Creating an asset and every status
 * change write an audit row in the same transaction.
 */
@Service
public class RoomAssetService {

    private final RoomAssetRepository assets;
    private final FacilityLookup lookup;
    private final AuditService audit;

    public RoomAssetService(RoomAssetRepository assets, FacilityLookup lookup, AuditService audit) {
        this.assets = assets;
        this.lookup = lookup;
        this.audit = audit;
    }

    @Transactional
    public RoomAssetResponse create(long roomId, RoomAssetRequest request) {
        Room room = lookup.room(roomId);
        RoomAsset asset = assets.save(new RoomAsset(room, request.name().trim(), request.status()));
        audit.record(AuditSubjectType.ROOM_ASSET, asset.getId(), (RoomAssetStatus) null, asset.getStatus());
        return RoomAssetResponse.from(asset);
    }

    /** Renames the asset and, when the status differs, records the status transition. */
    @Transactional
    public RoomAssetResponse update(long assetId, RoomAssetRequest request) {
        RoomAsset asset = lookup.asset(assetId);
        asset.rename(request.name().trim());
        RoomAssetStatus from = asset.getStatus();
        if (from != request.status()) {
            asset.changeStatus(request.status());
            audit.record(AuditSubjectType.ROOM_ASSET, assetId, from, request.status());
        }
        return RoomAssetResponse.from(asset);
    }

    @Transactional
    public void delete(long assetId) {
        assets.delete(lookup.asset(assetId));
    }
}
