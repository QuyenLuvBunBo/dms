package vn.edu.hust.dms.facility.service;

import java.util.Collection;
import java.util.Set;

/**
 * Tells the facility module which beds have an occupant, without it depending on the modules that
 * place students in beds. Every implementation is asked and the answers are combined
 * ({@link OccupancyService}); a bed is occupied as soon as one of them reports it.
 *
 * <p>Callers ask while holding PESSIMISTIC_WRITE locks on the beds, but their transaction may
 * already have a REPEATABLE READ snapshot. Implementations that read tables (Phase 2 onwards)
 * should therefore use a locking read (e.g. {@code SELECT ... FOR SHARE}) so they see rows committed
 * after that snapshot; the foreign keys to {@code beds} remain the backstop.
 */
public interface BedOccupancy {

    /** The ids among {@code bedIds} that currently have an occupant; never null. */
    Set<Long> occupiedBedIds(Collection<Long> bedIds);
}
