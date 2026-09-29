package vn.edu.hust.dms.facility.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Create and update take the same fields. The room's gender is not one of them (BR-03 sets it). */
public record RoomRequest(@NotBlank @Size(max = 16) String code, @NotNull Long roomTypeId) {
}
