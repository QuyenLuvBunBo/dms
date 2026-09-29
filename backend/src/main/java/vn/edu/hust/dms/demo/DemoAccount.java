package vn.edu.hust.dms.demo;

import vn.edu.hust.dms.common.user.Role;

import java.util.Optional;

/**
 * Demo logins created by the demo profile on an empty database: one per central role, and one
 * building manager per seeded building (linked to that building by {@link DemoDataService}).
 */
public enum DemoAccount {
    ADMIN("admin", "Trần Thị Mai", Role.ADMIN, null),
    STUDENT("student", "Nguyễn Văn An", Role.STUDENT, null),
    TECHNICIAN("technician", "Lê Văn Bình", Role.TECHNICIAN, null),
    ACCOUNTANT("accountant", "Phạm Thị Hoa", Role.ACCOUNTANT, null),
    MANAGER_B3("manager.b3", "Đỗ Văn Hùng", Role.BUILDING_MANAGER, "B3"),
    MANAGER_B5("manager.b5", "Vũ Thị Lan", Role.BUILDING_MANAGER, "B5"),
    MANAGER_B6("manager.b6", "Hoàng Minh Đức", Role.BUILDING_MANAGER, "B6"),
    MANAGER_B8("manager.b8", "Bùi Thị Thu", Role.BUILDING_MANAGER, "B8"),
    MANAGER_B9("manager.b9", "Đặng Văn Nam", Role.BUILDING_MANAGER, "B9"),
    MANAGER_B10("manager.b10", "Phan Thị Hằng", Role.BUILDING_MANAGER, "B10"),
    MANAGER_B13("manager.b13", "Trịnh Văn Long", Role.BUILDING_MANAGER, "B13");

    /** Shared by every demo account. */
    public static final String PASSWORD = "password";

    private final String username;
    private final String fullName;
    private final Role role;
    private final String buildingCode;

    DemoAccount(String username, String fullName, Role role, String buildingCode) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.buildingCode = buildingCode;
    }

    public String username() {
        return username;
    }

    public String fullName() {
        return fullName;
    }

    public Role role() {
        return role;
    }

    public String email() {
        return username + "@dms.example";
    }

    /** The building a demo manager is linked to; empty for the other accounts. */
    public Optional<String> buildingCode() {
        return Optional.ofNullable(buildingCode);
    }
}
