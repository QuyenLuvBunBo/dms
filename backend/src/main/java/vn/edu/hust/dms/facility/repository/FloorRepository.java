package vn.edu.hust.dms.facility.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.hust.dms.facility.entity.Floor;

import java.util.List;

public interface FloorRepository extends JpaRepository<Floor, Long> {

    List<Floor> findByBuildingIdOrderByNumberAsc(Long buildingId);

    boolean existsByBuildingIdAndNumber(Long buildingId, int number);

    boolean existsByBuildingIdAndNumberAndIdNot(Long buildingId, int number, Long id);

    boolean existsByBuildingId(Long buildingId);

    long countByBuildingId(Long buildingId);

    /** Rooms per floor of one building, as [floor id, room count]. */
    @Query("select r.floor.id, count(r) from Room r where r.floor.building.id = :buildingId group by r.floor.id")
    List<Object[]> countRoomsPerFloor(@Param("buildingId") Long buildingId);
}
