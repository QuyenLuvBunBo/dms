package vn.edu.hust.dms.facility.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vn.edu.hust.dms.facility.entity.RoomAssetStatus;

public record RoomAssetRequest(@NotBlank @Size(max = 128) String name, @NotNull RoomAssetStatus status) {
}
