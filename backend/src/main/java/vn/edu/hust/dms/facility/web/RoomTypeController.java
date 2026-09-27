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
import vn.edu.hust.dms.facility.service.RoomTypeService;

import java.util.List;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
public class RoomTypeController {

    private final RoomTypeService roomTypes;

    public RoomTypeController(RoomTypeService roomTypes) {
        this.roomTypes = roomTypes;
    }

    @GetMapping("/buildings/{buildingId}/room-types")
    public List<RoomTypeResponse> list(@PathVariable long buildingId) {
        return roomTypes.list(buildingId);
    }

    @PostMapping("/buildings/{buildingId}/room-types")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomTypeResponse create(@PathVariable long buildingId, @Valid @RequestBody RoomTypeRequest body) {
        return roomTypes.create(buildingId, body);
    }

    @PutMapping("/room-types/{id}")
    public RoomTypeResponse update(@PathVariable long id, @Valid @RequestBody RoomTypeRequest body) {
        return roomTypes.update(id, body);
    }

    @DeleteMapping("/room-types/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        roomTypes.delete(id);
    }
}
