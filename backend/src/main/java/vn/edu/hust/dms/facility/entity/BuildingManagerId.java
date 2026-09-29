package vn.edu.hust.dms.facility.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class BuildingManagerId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    protected BuildingManagerId() {
    }

    public BuildingManagerId(Long userId, Long buildingId) {
        this.userId = userId;
        this.buildingId = buildingId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BuildingManagerId that
                && Objects.equals(userId, that.userId)
                && Objects.equals(buildingId, that.buildingId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, buildingId);
    }
}
