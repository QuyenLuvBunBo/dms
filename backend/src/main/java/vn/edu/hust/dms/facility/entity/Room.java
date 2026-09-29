package vn.edu.hust.dms.facility.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    /** Unique within the building, e.g. 301. */
    @Column(nullable = false, length = 16)
    private String code;

    /**
     * Null while the room is empty; set and cleared by the registration rules (BR-03, Phase 2).
     * The facility API never writes it.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 10)
    private Gender gender;

    protected Room() {
    }

    public Room(Floor floor, RoomType roomType, String code) {
        this.floor = floor;
        this.roomType = roomType;
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public Floor getFloor() {
        return floor;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public String getCode() {
        return code;
    }

    public Gender getGender() {
        return gender;
    }

    public long buildingId() {
        return floor.getBuilding().getId();
    }

    public void update(String code, RoomType roomType) {
        this.code = code;
        this.roomType = roomType;
    }
}
