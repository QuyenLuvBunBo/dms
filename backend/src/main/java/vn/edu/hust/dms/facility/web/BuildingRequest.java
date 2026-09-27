package vn.edu.hust.dms.facility.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingRequest(@NotBlank @Size(max = 16) String code, @NotBlank @Size(max = 128) String name) {
}
