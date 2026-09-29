package vn.edu.hust.dms.facility.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.hust.dms.facility.entity.Building;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    /** Compared by the column collation, so B6 and b6 are the same code. */
    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);

    Optional<Building> findByCode(String code);

    List<Building> findByIdIn(Collection<Long> ids);
}
