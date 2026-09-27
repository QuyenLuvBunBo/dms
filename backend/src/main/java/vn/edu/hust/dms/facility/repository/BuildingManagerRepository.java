package vn.edu.hust.dms.facility.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.hust.dms.facility.entity.BuildingManager;
import vn.edu.hust.dms.facility.entity.BuildingManagerId;

import java.util.Collection;
import java.util.List;

public interface BuildingManagerRepository extends JpaRepository<BuildingManager, BuildingManagerId> {

    @Query("select m.id.buildingId from BuildingManager m where m.id.userId = :userId")
    List<Long> findBuildingIdsByUserId(@Param("userId") Long userId);

    boolean existsByIdUserIdAndIdBuildingId(Long userId, Long buildingId);

    List<BuildingManager> findByIdBuildingId(Long buildingId);

    List<BuildingManager> findByIdBuildingIdIn(Collection<Long> buildingIds);

    List<BuildingManager> findByIdUserIdIn(Collection<Long> userIds);

    @Modifying
    @Query("delete from BuildingManager m where m.id.buildingId = :buildingId")
    int deleteByBuilding(@Param("buildingId") Long buildingId);
}
