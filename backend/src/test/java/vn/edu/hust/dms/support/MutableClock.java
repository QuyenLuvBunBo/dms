package vn.edu.hust.dms.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** A clock tests can move by hand. Replaces the production Clock bean in the test context. */
public final class MutableClock extends Clock {

    private final Instant initial;
    private final ZoneId zone;
    private volatile Instant current;

    public MutableClock(Instant initial, ZoneId zone) {
        this.initial = initial;
        this.zone = zone;
        this.current = initial;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        MutableClock copy = new MutableClock(initial, zone);
        copy.current = current;
        return copy;
    }

    @Override
    public Instant instant() {
        return current;
    }

    public void set(Instant instant) {
        this.current = instant;
    }

    public void advance(Duration duration) {
        this.current = current.plus(duration);
    }

    public void advanceDays(long days) {
        advance(Duration.ofDays(days));
    }

    public void reset() {
        this.current = initial;
    }
}
