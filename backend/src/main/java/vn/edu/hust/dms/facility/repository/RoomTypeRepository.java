package vn.edu.hust.dms.facility.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.hust.dms.facility.entity.RoomType;

import java.util.List;
import java.util.Optional;

public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {

    /** The row locked for the rest of the transaction: capacity changes and bed additions serialise on it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RoomType t where t.id = :id")
    Optional<RoomType> findByIdForUpdate(@Param("id") Long id);

    List<RoomType> findByBuildingIdOrderByCapacityAscNameAsc(Long buildingId);

    boolean existsByBuildingIdAndName(Long buildingId, String name);

    boolean existsByBuildingIdAndNameAndIdNot(Long buildingId, String name, Long id);

    boolean existsByBuildingId(Long buildingId);

    /** Rooms per room type of one building, as [room type id, room count]. */
    @Query("select r.roomType.id, count(r) from Room r where r.roomType.building.id = :buildingId group by r.roomType.id")
    List<Object[]> countRoomsPerRoomType(@Param("buildingId") Long buildingId);
}
