package vn.edu.hust.dms.facility.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.dms.facility.service.BuildingService;

import java.util.List;

/**
 * Buildings. ADMIN and BUILDING_MANAGER pass the role check; the service applies the building
 * scope (BR-14) first and the ADMIN-only check second, so another building is always a 404.
 */
@RestController
@RequestMapping("/api/buildings")
@PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
public class BuildingController {

    private final BuildingService buildings;

    public BuildingController(BuildingService buildings) {
        this.buildings = buildings;
    }

    @GetMapping
    public List<BuildingResponse> list() {
        return buildings.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BuildingResponse create(@Valid @RequestBody BuildingRequest body) {
        return buildings.create(body);
    }

    @GetMapping("/{id}")
    public BuildingResponse get(@PathVariable long id) {
        return buildings.get(id);
    }

    @PutMapping("/{id}")
    public BuildingResponse update(@PathVariable long id, @Valid @RequestBody BuildingRequest body) {
        return buildings.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        buildings.delete(id);
    }
}
