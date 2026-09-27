package vn.edu.hust.dms.facility.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import vn.edu.hust.dms.common.error.NotFoundException;
import vn.edu.hust.dms.common.security.CurrentUserService;
import vn.edu.hust.dms.common.security.UserPrincipal;
import vn.edu.hust.dms.common.user.Role;
import vn.edu.hust.dms.facility.repository.BuildingManagerRepository;

import java.util.Set;

/**
 * BR-14 building scope. ADMIN sees every building; a BUILDING_MANAGER sees the buildings linked to
 * them in building_managers, read on every call so an unassignment applies to the next request;
 * any other role sees none. A building out of scope is reported as not found, with the same detail
 * as an id that does not exist, so its existence is not revealed. Later modules reuse this class.
 */
@Service
public class BuildingScope {

    private final CurrentUserService currentUser;
    private final BuildingManagerRepository managers;

    public BuildingScope(CurrentUserService currentUser, BuildingManagerRepository managers) {
        this.currentUser = currentUser;
        this.managers = managers;
    }

    /** What the current user may see: every building, or the listed ones. */
    public record Visibility(boolean all, Set<Long> buildingIds) {

        public boolean includes(long buildingId) {
            return all || buildingIds.contains(buildingId);
        }
    }

    public Visibility visible() {
        UserPrincipal user = currentUser.require();
        return switch (user.role()) {
            case ADMIN -> new Visibility(true, Set.of());
            case BUILDING_MANAGER -> new Visibility(false, Set.copyOf(managers.findBuildingIdsByUserId(user.id())));
            default -> new Visibility(false, Set.of());
        };
    }

    public boolean canSee(long buildingId) {
        UserPrincipal user = currentUser.require();
        return switch (user.role()) {
            case ADMIN -> true;
            case BUILDING_MANAGER -> managers.existsByIdUserIdAndIdBuildingId(user.id(), buildingId);
            default -> false;
        };
    }

    /** @throws NotFoundException with {@code notFoundDetail}, the detail a missing id gets too */
    public void requireVisible(long buildingId, String notFoundDetail) {
        if (!canSee(buildingId)) {
            throw new NotFoundException(notFoundDetail);
        }
    }

    /**
     * 403 for an ADMIN-only action. Call it after {@link #requireVisible}, so a manager asking about
     * another building gets 404 rather than 403.
     */
    public void requireAdmin() {
        if (currentUser.require().role() != Role.ADMIN) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
