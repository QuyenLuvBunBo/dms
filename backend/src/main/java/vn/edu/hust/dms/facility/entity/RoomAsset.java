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

/** One item of a room's inventory, e.g. an air conditioner. Status changes go through RoomAssetService. */
@Entity
@Table(name = "room_assets")
public class RoomAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private RoomAssetStatus status;

    protected RoomAsset() {
    }

    public RoomAsset(Room room, String name, RoomAssetStatus status) {
        this.room = room;
        this.name = name;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getName() {
        return name;
    }

    public RoomAssetStatus getStatus() {
        return status;
    }

    public void rename(String name) {
        this.name = name;
    }

    /** Called only by RoomAssetService, which audits the transition in the same transaction. */
    public void changeStatus(RoomAssetStatus status) {
        this.status = status;
    }
}
