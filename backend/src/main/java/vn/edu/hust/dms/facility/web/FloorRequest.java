package vn.edu.hust.dms.facility.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import vn.edu.hust.dms.facility.entity.Gender;

/** {@code genderPreference} is optional: null means no preference. */
public record FloorRequest(@NotNull @Min(1) @Max(99) Integer number, Gender genderPreference) {
}
