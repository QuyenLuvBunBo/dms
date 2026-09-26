package vn.edu.hust.dms.demo;

import vn.edu.hust.dms.common.user.Role;

/** One demo login per role, created by the demo profile on an empty database. */
public enum DemoAccount {
    ADMIN("admin", "Trần Thị Mai", Role.ADMIN),
    STUDENT("student", "Nguyễn Văn An", Role.STUDENT),
    TECHNICIAN("technician", "Lê Văn Bình", Role.TECHNICIAN),
    ACCOUNTANT("accountant", "Phạm Thị Hoa", Role.ACCOUNTANT),
    AFFAIRS("affairs", "Hoàng Minh Đức", Role.AFFAIRS);

    /** Shared by every demo account. */
    public static final String PASSWORD = "password";

    private final String username;
    private final String fullName;
    private final Role role;

    DemoAccount(String username, String fullName, Role role) {
        this.username = username;
        this.fullName = fullName;
        this.role = role;
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
}
