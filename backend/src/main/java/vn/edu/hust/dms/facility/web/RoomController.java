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
import vn.edu.hust.dms.facility.service.RoomService;

import java.util.List;

/** Rooms and their beds. */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
public class RoomController {

    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @GetMapping("/buildings/{buildingId}/rooms")
    public List<RoomSummaryResponse> list(@PathVariable long buildingId) {
        return rooms.list(buildingId);
    }

    @PostMapping("/floors/{floorId}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomDetailResponse create(@PathVariable long floorId, @Valid @RequestBody RoomRequest body) {
        return rooms.create(floorId, body);
    }

    @GetMapping("/rooms/{id}")
    public RoomDetailResponse get(@PathVariable long id) {
        return rooms.get(id);
    }

    @PutMapping("/rooms/{id}")
    public RoomDetailResponse update(@PathVariable long id, @Valid @RequestBody RoomRequest body) {
        return rooms.update(id, body);
    }

    @DeleteMapping("/rooms/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        rooms.delete(id);
    }

    @PostMapping("/rooms/{id}/beds")
    @ResponseStatus(HttpStatus.CREATED)
    public BedResponse addBed(@PathVariable long id, @Valid @RequestBody(required = false) AddBedRequest body) {
        return rooms.addBed(id, body);
    }

    @PutMapping("/beds/{id}")
    public BedResponse relabelBed(@PathVariable long id, @Valid @RequestBody BedRequest body) {
        return rooms.relabelBed(id, body);
    }

    @DeleteMapping("/beds/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeBed(@PathVariable long id) {
        rooms.removeBed(id);
    }
}
