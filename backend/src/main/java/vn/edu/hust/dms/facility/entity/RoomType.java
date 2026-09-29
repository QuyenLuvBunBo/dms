package vn.edu.hust.dms.facility.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "room_types")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false, length = 64)
    private String name;

    /** Number of beds a new room of this type gets; a room never has more. */
    @Column(nullable = false)
    private int capacity;

    @Column(name = "area_m2", nullable = false, precision = 6, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "has_air_conditioning", nullable = false)
    private boolean hasAirConditioning;

    @Column(name = "has_water_heater", nullable = false)
    private boolean hasWaterHeater;

    @Column(nullable = false)
    private int bathrooms;

    /** VND per student per month (BR-12). */
    @Column(name = "monthly_rent", nullable = false)
    private long monthlyRent;

    protected RoomType() {
    }

    public RoomType(Building building, Details details) {
        this.building = building;
        update(details);
    }

    /** The editable fields, as one value so create and update take the same input. */
    public record Details(String name, int capacity, BigDecimal areaM2, boolean hasAirConditioning,
                          boolean hasWaterHeater, int bathrooms, long monthlyRent) {
    }

    public Long getId() {
        return id;
    }

    public Building getBuilding() {
        return building;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getAreaM2() {
        return areaM2;
    }

    public boolean hasAirConditioning() {
        return hasAirConditioning;
    }

    public boolean hasWaterHeater() {
        return hasWaterHeater;
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public long getMonthlyRent() {
        return monthlyRent;
    }

    public void update(Details details) {
        this.name = details.name();
        this.capacity = details.capacity();
        this.areaM2 = details.areaM2();
        this.hasAirConditioning = details.hasAirConditioning();
        this.hasWaterHeater = details.hasWaterHeater();
        this.bathrooms = details.bathrooms();
        this.monthlyRent = details.monthlyRent();
    }
}
