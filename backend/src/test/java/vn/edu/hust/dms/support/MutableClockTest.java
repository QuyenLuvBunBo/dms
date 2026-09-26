package vn.edu.hust.dms.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class MutableClockTest {

    @Test
    @DisplayName("INFRA MutableClock advance() moves instant() forward and reset() returns to the initial instant")
    void advanceAndReset() {
        Instant start = Instant.parse("2026-09-01T00:00:00Z");
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        MutableClock clock = new MutableClock(start, zone);

        assertThat(clock.instant()).isEqualTo(start);
        assertThat(clock.getZone()).isEqualTo(zone);

        clock.advance(Duration.ofHours(49));
        assertThat(clock.instant()).isEqualTo(start.plus(Duration.ofHours(49)));
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 9, 3));

        clock.advanceDays(2);
        assertThat(clock.instant()).isEqualTo(start.plus(Duration.ofHours(97)));

        clock.set(start.minusSeconds(1));
        assertThat(clock.instant()).isEqualTo(start.minusSeconds(1));

        clock.reset();
        assertThat(clock.instant()).isEqualTo(start);
    }
}
