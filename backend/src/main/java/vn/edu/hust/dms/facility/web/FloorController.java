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
import vn.edu.hust.dms.facility.service.FloorService;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
public class FloorController {

    private final FloorService floors;

    public FloorController(FloorService floors) {
        this.floors = floors;
    }

    @GetMapping("/buildings/{buildingId}/floors")
    public List<FloorResponse> list(@PathVariable long buildingId) {
        return floors.list(buildingId);
    }

    @PostMapping("/buildings/{buildingId}/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public FloorResponse create(@PathVariable long buildingId, @Valid @RequestBody FloorRequest body) {
        return floors.create(buildingId, body);
    }

    @PutMapping("/floors/{id}")
    public FloorResponse update(@PathVariable long id, @Valid @RequestBody FloorRequest body) {
        return floors.update(id, body);
    }

    @DeleteMapping("/floors/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        floors.delete(id);
    }
}
