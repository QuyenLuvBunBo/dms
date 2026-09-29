package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The union of every {@link BedOccupancy}: a bed is occupied if any of them says so. A union rather
 * than a single bean, so a test-only source can run next to the real ones without hiding them.
 */
@Service
public class OccupancyService {

    private final List<BedOccupancy> sources;

    public OccupancyService(List<BedOccupancy> sources) {
        this.sources = List.copyOf(sources);
    }

    public Set<Long> occupiedBedIds(Collection<Long> bedIds) {
        if (bedIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> occupied = new HashSet<>();
        for (BedOccupancy source : sources) {
            occupied.addAll(source.occupiedBedIds(bedIds));
        }
        return occupied;
    }

    public boolean isOccupied(long bedId) {
        return !occupiedBedIds(List.of(bedId)).isEmpty();
    }
}
