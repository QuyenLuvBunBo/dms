package vn.edu.hust.dms.facility.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hust.dms.facility.entity.RoomAsset;

import java.util.List;

public interface RoomAssetRepository extends JpaRepository<RoomAsset, Long> {

    List<RoomAsset> findByRoomIdOrderByNameAscIdAsc(Long roomId);
}
