package vn.edu.hust.dms.common.user;

public enum Role {
    STUDENT,
    ADMIN,
    BUILDING_MANAGER,
    TECHNICIAN,
    ACCOUNTANT;

    /** Spring Security authority name, e.g. ROLE_ADMIN. */
    public String authority() {
        return "ROLE_" + name();
    }
}
