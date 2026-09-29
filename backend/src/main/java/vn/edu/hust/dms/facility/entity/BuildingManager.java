package vn.edu.hust.dms.facility.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Links a BUILDING_MANAGER account to a building it manages (BR-14). A manager may have several. */
@Entity
@Table(name = "building_managers")
public class BuildingManager {

    @EmbeddedId
    private BuildingManagerId id;

    protected BuildingManager() {
    }

    public BuildingManager(long userId, long buildingId) {
        this.id = new BuildingManagerId(userId, buildingId);
    }

    public BuildingManagerId getId() {
        return id;
    }

    public long getUserId() {
        return id.getUserId();
    }

    public long getBuildingId() {
        return id.getBuildingId();
    }
}
