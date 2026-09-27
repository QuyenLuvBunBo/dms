package vn.edu.hust.dms.common.settings;

import java.time.temporal.ChronoUnit;
import java.util.Optional;

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
    MONEY("an amount in VND, 0 or more") {
        @Override
        long parse(String value) {
            return atLeast(0, value);
        }
    },
    MINUTES("a whole number of minutes, 1 or more", ChronoUnit.MINUTES) {
        @Override
        long parse(String value) {
            return atLeast(1, value);
        }
    },
    HOURS("a whole number of hours, 0 or more", ChronoUnit.HOURS) {
        @Override
        long parse(String value) {
            return atLeast(0, value);
        }
    },
    DAYS("a whole number of days, 0 or more", ChronoUnit.DAYS) {
        @Override
        long parse(String value) {
            return atLeast(0, value);
        }
    };

    private final String description;
    private final ChronoUnit durationUnit;

    SettingType(String description) {
        this(description, null);
    }

    SettingType(String description, ChronoUnit durationUnit) {
        this.description = description;
        this.durationUnit = durationUnit;
    }

    public String description() {
        return description;
    }

    /** The unit of a duration type (MINUTES, HOURS, DAYS); empty for plain numbers and money. */
    public Optional<ChronoUnit> durationUnit() {
        return Optional.ofNullable(durationUnit);
    }

    /** @throws NumberFormatException when the value is not acceptable for this type */
    abstract long parse(String value);

    private static long atLeast(long minimum, String value) {
        long parsed = Long.parseLong(value);
        if (parsed < minimum) {
            throw new NumberFormatException("below " + minimum + ": " + value);
        }
        return parsed;
    }
}
