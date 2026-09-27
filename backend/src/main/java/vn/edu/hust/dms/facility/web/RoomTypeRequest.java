package vn.edu.hust.dms.facility.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import vn.edu.hust.dms.facility.entity.RoomType;

import java.math.BigDecimal;

/** Wrapper types, so a missing field is a 400 with a field error rather than a silent 0 or false. */
public record RoomTypeRequest(
        @NotBlank @Size(max = 64) String name,
        @NotNull @Min(1) @Max(50) Integer capacity,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 4, fraction = 2) BigDecimal areaM2,
        @NotNull Boolean hasAirConditioning,
        @NotNull Boolean hasWaterHeater,
        @NotNull @Min(0) @Max(20) Integer bathrooms,
        @NotNull @PositiveOrZero Long monthlyRent) {

    public RoomType.Details details() {
        return new RoomType.Details(name.trim(), capacity, areaM2, hasAirConditioning, hasWaterHeater, bathrooms,
                monthlyRent);
    }
}
