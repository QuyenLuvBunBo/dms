package vn.edu.hust.dms.facility.web;

import vn.edu.hust.dms.facility.entity.RoomType;

import java.math.BigDecimal;

/** {@code monthlyRent} is VND per student per month. */
public record RoomTypeResponse(long id, long buildingId, String name, int capacity, BigDecimal areaM2,
                               boolean hasAirConditioning, boolean hasWaterHeater, int bathrooms, long monthlyRent,
                               long roomCount) {

    public static RoomTypeResponse from(RoomType type, long roomCount) {
        return new RoomTypeResponse(type.getId(), type.getBuilding().getId(), type.getName(), type.getCapacity(),
                type.getAreaM2(), type.hasAirConditioning(), type.hasWaterHeater(), type.getBathrooms(),
                type.getMonthlyRent(), roomCount);
    }
}
