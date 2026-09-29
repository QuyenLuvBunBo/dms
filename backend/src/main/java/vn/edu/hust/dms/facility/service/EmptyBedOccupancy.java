package vn.edu.hust.dms.facility.service;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * The Phase 1 production {@link BedOccupancy}: it reports no bed as occupied, because no Phase 1
 * table can place a student in a bed. Room deletion and bed removal therefore never refuse for
 * occupancy in the running app yet. Phase 2 replaces this class with an implementation based on
 * registrations and residences (HELD, CONFIRMED and CHECKED_IN registrations, ACTIVE residences).
 */
@Component
public class EmptyBedOccupancy implements BedOccupancy {

    @Override
    public Set<Long> occupiedBedIds(Collection<Long> bedIds) {
        return Set.of();
    }
}
