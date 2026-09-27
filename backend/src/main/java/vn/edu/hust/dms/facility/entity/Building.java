package vn.edu.hust.dms.facility.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "buildings")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short code such as B6, unique (case-insensitive, by the column collation). */
    @Column(nullable = false, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    protected Building() {
    }

    public Building(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public void update(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
