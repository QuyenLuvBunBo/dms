package vn.edu.hust.dms.facility.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.dms.facility.service.RoomAssetService;

/** Room assets: the building manager of the room's building may change them too. */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','BUILDING_MANAGER')")
public class RoomAssetController {

    private final RoomAssetService assets;

    public RoomAssetController(RoomAssetService assets) {
        this.assets = assets;
    }

    @PostMapping("/rooms/{roomId}/assets")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomAssetResponse create(@PathVariable long roomId, @Valid @RequestBody RoomAssetRequest body) {
        return assets.create(roomId, body);
    }

    @PutMapping("/room-assets/{id}")
    public RoomAssetResponse update(@PathVariable long id, @Valid @RequestBody RoomAssetRequest body) {
        return assets.update(id, body);
    }

    @DeleteMapping("/room-assets/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        assets.delete(id);
    }
}
