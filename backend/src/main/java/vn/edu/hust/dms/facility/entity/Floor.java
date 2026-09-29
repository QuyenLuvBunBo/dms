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
@Table(name = "floors")
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(nullable = false)
    private int number;

    /** Only the default filter of the room browser; it never blocks a choice (BR-03). */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "gender_preference", length = 10)
    private Gender genderPreference;

    protected Floor() {
    }

    public Floor(Building building, int number, Gender genderPreference) {
        this.building = building;
        this.number = number;
        this.genderPreference = genderPreference;
    }

    public Long getId() {
        return id;
    }

    public Building getBuilding() {
        return building;
    }

    public int getNumber() {
        return number;
    }

    public Gender getGenderPreference() {
        return genderPreference;
    }

    public void update(int number, Gender genderPreference) {
        this.number = number;
        this.genderPreference = genderPreference;
    }
}
