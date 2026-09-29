package vn.edu.hust.dms.facility.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.hust.dms.facility.entity.Room;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    /** The rooms of a building with their floor and room type, ordered by floor then code. */
    @Query("select r from Room r join fetch r.floor f join fetch r.roomType"
            + " where f.building.id = :buildingId order by f.number, r.code")
    List<Room> findByBuildingWithFloorAndType(@Param("buildingId") Long buildingId);

    /** The room row locked for the rest of the transaction (bed changes against the capacity). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    boolean existsByFloorId(Long floorId);

    long countByFloorId(Long floorId);

    boolean existsByRoomTypeId(Long roomTypeId);

    long countByRoomTypeId(Long roomTypeId);

    /** Room codes are unique per building; the database constraint only covers one floor. */
    boolean existsByFloorBuildingIdAndCode(Long buildingId, String code);

    @Query("select case when count(r) > 0 then true else false end from Room r"
            + " where r.floor.building.id = :buildingId and r.code = :code and r.id <> :excludedId")
    boolean existsCodeInBuildingExcept(@Param("buildingId") Long buildingId, @Param("code") String code,
                                       @Param("excludedId") Long excludedId);
}
