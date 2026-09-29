package vn.edu.hust.dms.facility.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BedRequest(@NotBlank @Size(max = 8) String code) {
}
