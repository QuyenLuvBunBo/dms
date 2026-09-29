package vn.edu.hust.dms.facility.web;

import jakarta.validation.constraints.Size;

/** {@code code} is optional: without one the bed gets the lowest free number. */
public record AddBedRequest(@Size(max = 8) String code) {
}
