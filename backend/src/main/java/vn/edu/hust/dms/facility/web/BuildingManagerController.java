package vn.edu.hust.dms.facility.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.dms.facility.service.BuildingManagerService;

import java.util.List;

/**
 * Manager links. The link endpoints admit building managers at the role check only so that the
 * service can answer 404 for other buildings (BR-14) before its ADMIN-only check.
 */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
public class BuildingManagerController {

    private final BuildingManagerService managers;

    public BuildingManagerController(BuildingManagerService managers) {
        this.managers = managers;
    }

    @GetMapping("/building-managers")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BuildingManagerResponse> list() {
        return managers.list();
    }

    @PutMapping("/buildings/{buildingId}/managers/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assign(@PathVariable long buildingId, @PathVariable long userId) {
        managers.assign(buildingId, userId);
    }

    @DeleteMapping("/buildings/{buildingId}/managers/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@PathVariable long buildingId, @PathVariable long userId) {
        managers.unassign(buildingId, userId);
    }
}
