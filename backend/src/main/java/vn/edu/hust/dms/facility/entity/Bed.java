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

@Entity
@Table(name = "beds")
public class Bed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    /** Unique within the room; new rooms get 1..capacity. */
    @Column(nullable = false, length = 8)
    private String code;

    protected Bed() {
    }

    public Bed(Room room, String code) {
        this.room = room;
        this.code = code;
    }

    public Long getId() {
        return id;
    }

    public Room getRoom() {
        return room;
    }

    public String getCode() {
        return code;
    }

    public void relabel(String code) {
        this.code = code;
    }
}
