package vn.edu.hust.dms.facility.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.hust.dms.facility.entity.Bed;

import java.util.List;
import java.util.Optional;

public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByRoomId(Long roomId);

    long countByRoomId(Long roomId);

    boolean existsByRoomIdAndCode(Long roomId, String code);

    boolean existsByRoomIdAndCodeAndIdNot(Long roomId, String code, Long id);

    /** Locks the bed row: occupancy checks and holds (BR-04, Phase 2) serialise on it. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bed b where b.id = :id")
    Optional<Bed> findByIdForUpdate(@Param("id") Long id);

    /** Locks every bed of the room, in id order so concurrent lockers cannot deadlock. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Bed b where b.room.id = :roomId order by b.id")
    List<Bed> findByRoomIdForUpdate(@Param("roomId") Long roomId);

    /** Every bed of a building as [bed id, room id], for bed and occupancy counts per room. */
    @Query("select b.id, b.room.id from Bed b where b.room.floor.building.id = :buildingId")
    List<Object[]> findBedAndRoomIdsByBuilding(@Param("buildingId") Long buildingId);

    /** The bed count of every room of a room type that has beds (empty when the type is unused). */
    @Query("select count(b) from Bed b where b.room.roomType.id = :roomTypeId group by b.room.id")
    List<Long> bedCountsPerRoomOfType(@Param("roomTypeId") Long roomTypeId);
}
