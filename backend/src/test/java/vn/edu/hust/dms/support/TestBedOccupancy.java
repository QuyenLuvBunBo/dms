package vn.edu.hust.dms.support;

import org.springframework.stereotype.Component;
import vn.edu.hust.dms.facility.service.BedOccupancy;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Test-only occupant source: a test marks beds occupied by hand. It runs next to the production
 * implementations (the answers are combined), and AbstractIntegrationTest clears it before each test.
 */
@Component
public class TestBedOccupancy implements BedOccupancy {

    private final Set<Long> occupied = ConcurrentHashMap.newKeySet();

    public void occupy(long bedId) {
        occupied.add(bedId);
    }

    public void clear() {
        occupied.clear();
    }

    @Override
    public Set<Long> occupiedBedIds(Collection<Long> bedIds) {
        return bedIds.stream().filter(occupied::contains).collect(Collectors.toSet());
    }
}
