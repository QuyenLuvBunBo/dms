package vn.edu.hust.dms.common.settings;

/** Value type of a setting; drives validation on update and the typed getters. */
public enum SettingType {
    INTEGER("a whole number") {
        @Override
        long parse(String value) {
            return Integer.parseInt(value);
        }
    },
    LONG("a whole number") {
        @Override
        long parse(String value) {
            return Long.parseLong(value);
        }
    },
    HOURS("a whole number of hours, 0 or more") {
        @Override
        long parse(String value) {
            long hours = Long.parseLong(value);
            if (hours < 0) {
                throw new NumberFormatException("negative hours: " + value);
            }
            return hours;
        }
    };

    private final String description;

    SettingType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    /** @throws NumberFormatException when the value is not acceptable for this type */
    abstract long parse(String value);
}
