package vn.edu.hust.dms.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneId;

/** Registers the mutable clock as the primary Clock so every service under test reads it. */
@TestConfiguration
public class TestClockConfig {

    /** 2026-09-01 08:00 in Ho Chi Minh City. */
    public static final Instant INITIAL = Instant.parse("2026-09-01T01:00:00Z");

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock(INITIAL, ZONE);
    }
}
